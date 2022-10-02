/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.apache.metron.parsers;

import com.google.common.base.Joiner;
import com.google.common.base.Splitter;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Serializable;
import java.io.StringReader;
import java.lang.invoke.MethodHandles;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.TimeZone;
import oi.thekraken.grok.api.Grok;
import oi.thekraken.grok.api.Match;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.metron.stellar.common.Constants;
import org.apache.metron.common.utils.LazyLogger;
import org.apache.metron.common.utils.LazyLoggerFactory;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.json.simple.JSONObject;


public class GrokParser implements MessageParser<JSONObject>, Serializable {

  protected static final LazyLogger LOG = LazyLoggerFactory.getLogger(MethodHandles.lookup().lookupClass());

  protected transient Grok grok;
  protected String grokPath;
  protected boolean multiLine = false;
  protected String patternLabel;
  protected List<String> timeFields = new ArrayList<>();
  protected String timestampField;
  protected SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.S z");
  protected String patternsCommonDir = "/patterns/common";
  private Charset readCharset;

  @Override
  @SuppressWarnings("unchecked")
  public void configure(Map<String, Object> parserConfig) {
    setReadCharset(parserConfig);
    this.grokPath = (String) parserConfig.get("grokPath");
    String multiLineString = (String) parserConfig.get("multiLine");
    if (!StringUtils.isBlank(multiLineString)) {
      multiLine = Boolean.parseBoolean(multiLineString);
    }
    this.patternLabel = (String) parserConfig.get("patternLabel");
    this.timestampField = (String) parserConfig.get("timestampField");
    List<String> timeFieldsParam = (List<String>) parserConfig.get("timeFields");
    if (timeFieldsParam != null) {
      this.timeFields = timeFieldsParam;
    }
    String dateFormatParam = (String) parserConfig.get("dateFormat");
    if (dateFormatParam != null) {
      this.dateFormat = new SimpleDateFormat(dateFormatParam);
    }
    String timeZoneParam = (String) parserConfig.get("timeZone");
    if (timeZoneParam != null) {
      dateFormat.setTimeZone(TimeZone.getTimeZone(timeZoneParam));
      LOG.debug("Grok Parser using provided TimeZone: {}", timeZoneParam);
    } else {
      dateFormat.setTimeZone(TimeZone.getTimeZone("UTC"));
      LOG.debug("Grok Parser using default TimeZone (UTC)");
    }
  }

  public InputStream openInputStream(String streamName) throws IOException {
    Path path = new Path(streamName);
    FileSystem fs = path.getFileSystem();
    if (fs.exists(path)) {
      LOG.info("Loading {} from file system.", streamName);
      return fs.open(path);
    } else {
      LOG.info("File not found in HDFS, attempting to load {} from classpath using classloader for {}.", streamName, getClass());
      return getClass().getResourceAsStream(streamName);
    }
  }

  @Override
  public void init() {
    grok = new Grok();
    try {
      InputStream commonInputStream = openInputStream(patternsCommonDir);
      LOG.info("Grok parser loading common patterns from: {}", patternsCommonDir);

      if (commonInputStream == null) {
        throw new RuntimeException(
                "Unable to initialize grok parser: Unable to load " + patternsCommonDir + " from either classpath or HDFS");
      }

      grok.addPatternFromReader(new InputStreamReader(commonInputStream, getReadCharset()));
      LOG.info("Loading parser-specific patterns from: {}", grokPath);

      InputStream patterInputStream = openInputStream(grokPath);
      if (patterInputStream == null) {
        throw new RuntimeException("Grok parser unable to initialize grok parser: Unable to load " + grokPath
                + " from either classpath or HDFS");
      }
      grok.addPatternFromReader(new InputStreamReader(patterInputStream, getReadCharset()));

      LOG.info("Grok parser set the following grok expression for '{}': {}", () ->patternLabel,
              () -> grok.getPatterns().get(patternLabel));

      String grokPattern = "%{" + patternLabel + "}";

      grok.compile(grokPattern);
      LOG.info("Compiled grok pattern {}", grokPattern);

    } catch (Throwable e) {
      LOG.error(e.getMessage(), e);
      throw new RuntimeException("Grok parser Error: " + e.getMessage(), e);
    }
  }

  @Override
  public Optional<MessageParserResult<JSONObject>> parseOptionalResult(byte[] rawMessage) {
    if (grok == null) {
      init();
    }
    if (multiLine) {
      return parseMultiLine(rawMessage);
    }
    return parseSingleLine(rawMessage);
  }

  @SuppressWarnings("unchecked")
  private Optional<MessageParserResult<JSONObject>> parseMultiLine(byte[] rawMessage) {
    List<JSONObject> messages = new ArrayList<>();
    Map<Object,Throwable> errors = new HashMap<>();
    String originalMessage = null;
    // read the incoming raw data as if it may have multiple lines of logs
    // if there is only only one line, it will just get processed.
    try (BufferedReader reader = new BufferedReader(new StringReader(new String(rawMessage, getReadCharset())))) {
      while ((originalMessage = reader.readLine()) != null) {
        LOG.debug("Grok parser parsing message: {}", originalMessage);
        try {
          Match gm = grok.match(originalMessage);
          gm.captures();
          JSONObject message = new JSONObject();
          message.putAll(gm.toMap());

          if (message.size() == 0) {
            Throwable rte = new RuntimeException("Grok statement produced a null message. Original message was: "
                    + originalMessage + " and the parsed message was: " + message + " . Check the pattern at: "
                    + grokPath);
            errors.put(originalMessage, rte);
            continue;
          }
          message.put("original_string", originalMessage);
          for (String timeField : timeFields) {
            String fieldValue = (String) message.get(timeField);
            if (fieldValue != null) {
              message.put(timeField, toEpoch(fieldValue));
            }
          }
          if (timestampField != null) {
            message.put(Constants.Fields.TIMESTAMP.getName(), formatTimestamp(message.get(timestampField)));
          }
          message.remove(patternLabel);
          postParse(message);
          messages.add(message);
          LOG.debug("Grok parser parsed message: {}", message);
        } catch (Exception e) {
          LOG.error(e.getMessage(), e);
          errors.put(originalMessage, e);
        }
      }
    } catch (IOException e) {
      LOG.error(e.getMessage(), e);
      Exception innerException = new IllegalStateException("Grok parser Error: "
              + e.getMessage()
              + " on "
              + originalMessage, e);
      return Optional.of(new DefaultMessageParserResult<>(innerException));
    }
    return Optional.of(new DefaultMessageParserResult<>(messages, errors));
  }

  @SuppressWarnings("unchecked")
  private Optional<MessageParserResult<JSONObject>> parseSingleLine(byte[] rawMessage) {
    List<JSONObject> messages = new ArrayList<>();
    Map<Object,Throwable> errors = new HashMap<>();
    String originalMessage = null;
    try {
      originalMessage = new String(rawMessage, StandardCharsets.UTF_8);
      LOG.debug("Grok parser parsing message: {}",originalMessage);
      Match gm = grok.match(originalMessage);
      gm.captures();
      JSONObject message = new JSONObject();
      message.putAll(gm.toMap());

      if (message.size() == 0) {
        Throwable rte = new RuntimeException("Grok statement produced a null message. Original message was: "
                + originalMessage + " and the parsed message was: " + message + " . Check the pattern at: "
                + grokPath);
        errors.put(originalMessage, rte);
      } else {
        message.put("original_string", originalMessage);
        for (String timeField : timeFields) {
          String fieldValue = (String) message.get(timeField);
          if (fieldValue != null) {
            message.put(timeField, toEpoch(fieldValue));
          }
        }
        if (timestampField != null) {
          message.put(Constants.Fields.TIMESTAMP.getName(), formatTimestamp(message.get(timestampField)));
        }
        message.remove(patternLabel);
        postParse(message);
        messages.add(message);
        LOG.debug("Grok parser parsed message: {}", message);
      }
    } catch (Exception e) {
      LOG.error(e.getMessage(), e);
      Exception innerException = new IllegalStateException("Grok parser Error: "
              + e.getMessage()
              + " on "
              + originalMessage, e);
      return Optional.of(new DefaultMessageParserResult<>(innerException));
    }
    return Optional.of(new DefaultMessageParserResult<>(messages, errors));
  }

  @Override
  public boolean validate(JSONObject message) {
    LOG.debug("Grok parser validating message: {}", message);

    Object timestampObject = message.get(Constants.Fields.TIMESTAMP.getName());
    if (timestampObject instanceof Long) {
      Long timestamp = (Long) timestampObject;
      if (timestamp > 0) {
        LOG.debug("Grok parser validated message: {}", message);
        return true;
      }
    }

    LOG.debug("Grok parser did not validate message: {}", message);
    return false;
  }

  protected void postParse(JSONObject message) {}

  protected long toEpoch(String datetime) throws ParseException {
    LOG.debug("Grok parser converting timestamp to epoch: {}", datetime);
    LOG.debug("Grok parser's DateFormat has TimeZone: {}", () -> dateFormat.getTimeZone());

    Date date = dateFormat.parse(datetime);
    LOG.debug("Grok parser converted timestamp to epoch: {}", date);

    return date.getTime();
  }

  protected long formatTimestamp(Object value) {
    LOG.debug("Grok parser formatting timestamp {}", value);

    if (value == null) {
      throw new RuntimeException(patternLabel + " pattern does not include field " + timestampField);
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    } else {
      return Long.parseLong(Joiner.on("").join(Splitter.on('.').split(value + "")));
    }
  }

  public void setReadCharset(Map<String, Object> config) {
    if (config.containsKey(READ_CHARSET)) {
      readCharset = Charset.forName((String) config.get(READ_CHARSET));
    } else {
      readCharset = MessageParser.super.getReadCharset();
    }
  }

  @Override
  public Charset getReadCharset() {
    return null == this.readCharset ? MessageParser.super.getReadCharset() : this.readCharset;
  }

}
