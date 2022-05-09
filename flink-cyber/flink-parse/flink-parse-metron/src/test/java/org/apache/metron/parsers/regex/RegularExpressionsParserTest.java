/**
 * Licensed to the Apache Software Foundation (ASF) under one or more contributor license
 * agreements. See the NOTICE file distributed with this work for additional information regarding
 * copyright ownership. The ASF licenses this file to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance with the License. You may obtain a
 * copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package org.apache.metron.parsers.regex;

import org.adrianwalker.multilinestring.Multiline;
import org.apache.metron.parsers.interfaces.MessageParser;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

public class RegularExpressionsParserTest {

    private RegularExpressionsParser regularExpressionsParser;

    @BeforeEach
    public void setUp() {
        regularExpressionsParser = new RegularExpressionsParser();
    }

    //@formatter:off
      /**
       {
          "convertCamelCaseToUnderScore": true,
          "messageHeaderRegex": "(?<syslogpriority>(?<=^<)\\d{1,4}(?=>)).*?(?<timestampDeviceOriginal>(?<=>)[A-Za-z]{3}\\s{1,2}\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}(?=\\s)).*?(?<deviceName>(?<=\\s).*?(?=\\s))",
          "recordTypeRegex": "(?<dstProcessName>(?<=\\s)\\b(kesl|sshd|run-parts|kernel|vsftpd|ftpd|su)\\b(?=\\[|:))",
          "fields": [
            {
              "recordType": "kesl",
              "regex": ".*(?<eventInfo>(?<=\\:).*?(?=$))"
            },
            {
              "recordType": "run-parts",
              "regex": ".*(?<eventInfo>(?<=\\sparts).*?(?=$))"
            },
            {
              "recordType": "sshd",
              "regex": [
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s).*?(?=\\sfor)).*?(?<dstUserId>(?<=\\sfor\\s).*?(?=\\sfrom)).*?(?<ipSrcAddr>(?<=\\sfrom\\s).*?(?=\\sport)).*?(?<ipSrcPort>(?<=\\sport\\s).*?(?=\\s)).*?(?<appProtocol>(?<=port\\s\\d{1,5}\\s).*(?=:\\s)).*?(?<encryptionAlgorithm>(?<=:\\s).+?(?=\\s)).*(?<correlationId>(?<=\\s).+?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s).*?(?=\\sfor)).*?(?<dstUserId>(?<=\\sfor\\s).*?(?=\\sfrom)).*?(?<ipSrcAddr>(?<=\\sfrom\\s).*?(?=\\sport)).*?(?<ipSrcPort>(?<=\\sport\\s).*?(?=\\s)).*?(?<appProtocol>(?<=port\\s\\d{1,5}\\s).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<ipDstAddr>(?<=Remote:).*?(?=\\-)).*?(?<ipDstPort>(?<=\\-).*?(?=;)).*?(?<appProtocol>(?<=Protocol:).*?(?=;)).*?(?<sshClient>(?<=Client:).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<appProtocol>(?<=\\]:).*?(?=:)).*?(?<ipDstAddr>(?<=Remote:).*?(?=\\-)).*?(?<ipDstPort>(?<=\\-).*?(?=;)).*?(?<encryptionAlgorithm>(?<=Enc:\\s).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<ipDstAddr>(?<=Remote:).*?(?=\\-)).*?(?<ipDstPort>(?<=\\-).*?(?=;)).*?(?<encryptionAlgorithm>(?<=Enc:\\s).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=for)).*?(?<dstUserId>(?<=for).*?(?=from)).*?(?<ipSrcAddr>(?<=from).*?(?=port)).*?(?<ipSrcPort>(?<=port).*?(?=\\s)).*?(?<appProtocol>(?<=\\s).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\]))]:\\s.*?(?<eventInfo>subsystem.*?(?=by\\suser)).*?(?<srcUserId>(?<=user).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<action>(?<=Received).*?(?=from)).*?(?<ipSrcAddr>(?<=from).*?(?=:)).*?(?<eventInfo>(?<=11:).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s)Server\\slistening(?=\\s)).*?(?<ipSrcAddr>(?<=\\son\\s).*?(?=port)).*?(?<ipSrcPort>(?<=port\\s)\\d{1,6}(?=\\.)).*$",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s)Invalid user(?=\\s)).*?(?<dstUserId>(?<=\\s).*?(?=from)).*?(?<ipSrcAddr>(?<=from\\s).*(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<subProcess>(?<=]:\\s).*\\)(?=:)).*(?<eventInfo>(?<=:\\s).*(?=;)).*(?<logname>(?<=logname=).*?(?=\\s)).*(?<dstUserId>(?<=uid=).*?(?=\\s)).*(?<effectiveUserId>(?<=euid=).*?(?=\\s)).*(?<sessionName>(?<=tty=).*?(?=\\s)).*(?<srcUserId>(?<=ruser=).*?(?=\\s)).*(?<ipSrcAddr>(?<=rhost=).*?(?=\\s)).*(?<userId>(?<=user=).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<eventInfo>(?<=:\\s).*(?=;)).*(?<logname>(?<=logname=).*?(?=\\s)).*(?<dstUserId>(?<=uid=).*?(?=\\s)).*(?<effectiveUserId>(?<=euid=).*?(?=\\s)).*(?<sessionName>(?<=tty=).*?(?=\\s)).*(?<srcUserId>(?<=ruser=).*?(?=\\s)).*(?<ipSrcAddr>(?<=rhost=).*?(?=\\s)).*(?<userId>(?<=user=).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s).*?(?=for)).*?(?<dstUserId>(?<=\\sfor).*?(?=\\[)).*?(?<subProcess>(?<=\\[).*?(?=\\])).*$",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:\\s)Excess permission or bad ownership on file(?=\\s\\/)).*?(?<filePath>(?<=\\s).*(?=\\/)).*?(?<fileName>(?<=\\/).*(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=;)).*$",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=\\d)).*$",
                ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=$))"
              ]
            },
            {
              "recordType": "kernel",
              "regex": [
                ".*(?<connectedDeviceName>(?<=\\:\\susb).*?(?=\\:)).*?(?<eventInfo>(?<=\\:).*?(?=$))",
                ".*(?<subProcess>(?<=\\:\\s).*?(?=\\:)).*?(?<eventInfo>(?<=\\:).*?(?=$))"
              ]
            },
            {
              "recordType": "vsftpd",
              "regex": ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<subProcess>(?<=]:\\s).*\\)(?=:)).*(?<eventInfo>(?<=:\\s).*(?=;)).*(?<effectiveUserId>(?<=euid=).*?(?=\\s)).*(?<sessionName>(?<=tty=).*?(?=\\s)).*(?<srcUserId>(?<=user=).*?(?=\\s)).*(?<ipSrcAddr>(?<=rhost=).*?(?=\\s)).*(?<dstUserId>(?<=user=).*?(?=$))"
            },
            {
              "recordType": "ftpd",
              "regex": [
                ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<eventInfo>(?<=:\\s).*(?=FROM)).*(?<srcHost>(?<=\\s).*?(?=\\s)).*(?<ipSrcAddr>(?<=\\s).*?(?=,)).*(?<dstUserId>(?<=,).*?(?=$))",
                ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<eventInfo>(?<=:\\s).*(?=from)).*(?<srcHost>(?<=\\s).*?(?=\\s)).*(?<ipSrcAddr>(?<=\\s).*?(?=,)).*(?<dstUserId>(?<=,).*?(?=$))"
              ]
            },
            {
              "recordType": "su",
              "regex": [
                ".*(?<eventInfo>(?<=:\\s).*(?=for)).*(?<dstUserId>(?<=user=).*?(?=to)).*(?<responseCode>(?<=to).*?(?=$))"
              ]
            }
          ]
      }
      */
    @Multiline
    public static String parserConfig1;
    //@formatter:on


    @Test
    public void testSSHDParse() throws Exception {
        String message =
            "<38>Jun 20 15:01:17 deviceName sshd[11672]: Accepted publickey for prod from 22.22.22.22 port 55555 ssh2";

        JSONObject parserConfig = (JSONObject) new JSONParser().parse(parserConfig1);
        regularExpressionsParser.configure(parserConfig);
        JSONObject parsed = parse(message);
        // Expected
        Map<String, Object> expectedJson = new HashMap<>();
        assertEquals(parsed.get("device_name"), "deviceName");
        assertEquals(parsed.get("dst_process_name"), "sshd");
        assertEquals(parsed.get("dst_process_id"), "11672");
        assertEquals(parsed.get("dst_user_id"), "prod");
        assertEquals(parsed.get("ip_src_addr"), "22.22.22.22");
        assertEquals(parsed.get("ip_src_port"), "55555");
        assertEquals(parsed.get("app_protocol"), "ssh2");
        assertEquals(parsed.get("original_string"),
            "<38>Jun 20 15:01:17 deviceName sshd[11672]: Accepted publickey for prod from 22.22.22.22 port 55555 ssh2");
        assertTrue(parsed.containsKey("timestamp"));

    }

    //@formatter:off
    /**
    {
    "convertCamelCaseToUnderScore": true,
    "recordTypeRegex": "(?<dstProcessName>(?<=\\s)\\b(kesl|sshd|run-parts|kernel|vsftpd|ftpd|su)\\b(?=\\[|:))",
    "fields": [
      {
        "recordType": "kesl",
        "regex": ".*(?<eventInfo>(?<=\\:).*?(?=$))"
      },
      {
        "recordType": "run-parts",
        "regex": ".*(?<eventInfo>(?<=\\sparts).*?(?=$))"
      },
      {
        "recordType": "sshd",
        "regex": [
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s).*?(?=\\sfor)).*?(?<dstUserId>(?<=\\sfor\\s).*?(?=\\sfrom)).*?(?<ipSrcAddr>(?<=\\sfrom\\s).*?(?=\\sport)).*?(?<ipSrcPort>(?<=\\sport\\s).*?(?=\\s)).*?(?<appProtocol>(?<=port\\s\\d{1,5}\\s).*(?=:\\s)).*?(?<encryptionAlgorithm>(?<=:\\s).+?(?=\\s)).*(?<correlationId>(?<=\\s).+?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s).*?(?=\\sfor)).*?(?<dstUserId>(?<=\\sfor\\s).*?(?=\\sfrom)).*?(?<ipSrcAddr>(?<=\\sfrom\\s).*?(?=\\sport)).*?(?<ipSrcPort>(?<=\\sport\\s).*?(?=\\s)).*?(?<appProtocol>(?<=port\\s\\d{1,5}\\s).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<ipDstAddr>(?<=Remote:).*?(?=\\-)).*?(?<ipDstPort>(?<=\\-).*?(?=;)).*?(?<appProtocol>(?<=Protocol:).*?(?=;)).*?(?<sshClient>(?<=Client:).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<appProtocol>(?<=\\]:).*?(?=:)).*?(?<ipDstAddr>(?<=Remote:).*?(?=\\-)).*?(?<ipDstPort>(?<=\\-).*?(?=;)).*?(?<encryptionAlgorithm>(?<=Enc:\\s).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<ipDstAddr>(?<=Remote:).*?(?=\\-)).*?(?<ipDstPort>(?<=\\-).*?(?=;)).*?(?<encryptionAlgorithm>(?<=Enc:\\s).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=for)).*?(?<dstUserId>(?<=for).*?(?=from)).*?(?<ipSrcAddr>(?<=from).*?(?=port)).*?(?<ipSrcPort>(?<=port).*?(?=\\s)).*?(?<appProtocol>(?<=\\s).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\]))]:\\s.*?(?<eventInfo>subsystem.*?(?=by\\suser)).*?(?<srcUserId>(?<=user).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<action>(?<=Received).*?(?=from)).*?(?<ipSrcAddr>(?<=from).*?(?=:)).*?(?<eventInfo>(?<=11:).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s)Server\\slistening(?=\\s)).*?(?<ipSrcAddr>(?<=\\son\\s).*?(?=port)).*?(?<ipSrcPort>(?<=port\\s)\\d{1,6}(?=\\.)).*$",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s)Invalid user(?=\\s)).*?(?<dstUserId>(?<=\\s).*?(?=from)).*?(?<ipSrcAddr>(?<=from\\s).*(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<subProcess>(?<=]:\\s).*\\)(?=:)).*(?<eventInfo>(?<=:\\s).*(?=;)).*(?<logname>(?<=logname=).*?(?=\\s)).*(?<dstUserId>(?<=uid=).*?(?=\\s)).*(?<effectiveUserId>(?<=euid=).*?(?=\\s)).*(?<sessionName>(?<=tty=).*?(?=\\s)).*(?<srcUserId>(?<=ruser=).*?(?=\\s)).*(?<ipSrcAddr>(?<=rhost=).*?(?=\\s)).*(?<userId>(?<=user=).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<eventInfo>(?<=:\\s).*(?=;)).*(?<logname>(?<=logname=).*?(?=\\s)).*(?<dstUserId>(?<=uid=).*?(?=\\s)).*(?<effectiveUserId>(?<=euid=).*?(?=\\s)).*(?<sessionName>(?<=tty=).*?(?=\\s)).*(?<srcUserId>(?<=ruser=).*?(?=\\s)).*(?<ipSrcAddr>(?<=rhost=).*?(?=\\s)).*(?<userId>(?<=user=).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=\\]:\\s).*?(?=for)).*?(?<dstUserId>(?<=\\sfor).*?(?=\\[)).*?(?<subProcess>(?<=\\[).*?(?=\\])).*$",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:\\s)Excess permission or bad ownership on file(?=\\s\\/)).*?(?<filePath>(?<=\\s).*(?=\\/)).*?(?<fileName>(?<=\\/).*(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=;)).*$",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=\\d)).*$",
          ".*(?<dstProcessId>(?<=\\[).*?(?=\\])).*?(?<eventInfo>(?<=:).*?(?=$))"
        ]
      },
      {
        "recordType": "kernel",
        "regex": [
          ".*(?<connectedDeviceName>(?<=\\:\\susb).*?(?=\\:)).*?(?<eventInfo>(?<=\\:).*?(?=$))",
          ".*(?<subProcess>(?<=\\:\\s).*?(?=\\:)).*?(?<eventInfo>(?<=\\:).*?(?=$))"
        ]
      },
      {
        "recordType": "vsftpd",
        "regex": ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<subProcess>(?<=]:\\s).*\\)(?=:)).*(?<eventInfo>(?<=:\\s).*(?=;)).*(?<effectiveUserId>(?<=euid=).*?(?=\\s)).*(?<sessionName>(?<=tty=).*?(?=\\s)).*(?<srcUserId>(?<=user=).*?(?=\\s)).*(?<ipSrcAddr>(?<=rhost=).*?(?=\\s)).*(?<dstUserId>(?<=user=).*?(?=$))"
      },
      {
        "recordType": "ftpd",
        "regex": [
          ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<eventInfo>(?<=:\\s).*(?=FROM)).*(?<srcHost>(?<=\\s).*?(?=\\s)).*(?<ipSrcAddr>(?<=\\s).*?(?=,)).*(?<dstUserId>(?<=,).*?(?=$))",
          ".*(?<dstProcessId>(?<=\\[).*?(?=]:\\s)).*(?<eventInfo>(?<=:\\s).*(?=from)).*(?<srcHost>(?<=\\s).*?(?=\\s)).*(?<ipSrcAddr>(?<=\\s).*?(?=,)).*(?<dstUserId>(?<=,).*?(?=$))"
        ]
      },
      {
        "recordType": "su",
        "regex": [
          ".*(?<eventInfo>(?<=:\\s).*(?=for)).*(?<dstUserId>(?<=user=).*?(?=to)).*(?<responseCode>(?<=to).*?(?=$))"
        ]
      }
    ]
    }
    */
    @Multiline
    public static String parserConfigNoMessageHeader;
    //@formatter:on

    @Test
    public void testNoMessageHeaderRegex() throws Exception {
        String message =
            "<38>Jun 20 15:01:17 deviceName sshd[11672]: Accepted publickey for prod from 22.22.22.22 port 55555 ssh2";
        JSONObject parserConfig = (JSONObject) new JSONParser().parse(parserConfigNoMessageHeader);
        regularExpressionsParser.configure(parserConfig);
        JSONObject parsed = parse(message);
        // Expected

        assertEquals(parsed.get("dst_process_name"), "sshd");
        assertEquals(parsed.get("dst_process_id"), "11672");
        assertEquals(parsed.get("dst_user_id"), "prod");
        assertEquals(parsed.get("ip_src_addr"), "22.22.22.22");
        assertEquals(parsed.get("ip_src_port"), "55555");
        assertEquals(parsed.get("app_protocol"), "ssh2");
        assertEquals(parsed.get("original_string"),
            "<38>Jun 20 15:01:17 deviceName sshd[11672]: Accepted publickey for prod from 22.22.22.22 port 55555 ssh2");
        assertTrue(parsed.containsKey("timestamp"));

    }

    //@formatter:off
    /**
        {
            "messageHeaderRegex": "(?<syslog_priority>(?<=^<)\\d{1,4}(?=>)).*?(?<timestampDeviceOriginal>(?<=>)[A-Za-z]{3}\\s{1,2}\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}(?=\\s)).*?(?<deviceName>(?<=\\s).*?(?=\\s))",
            "recordTypeRegex": "(?<dstProcessName>(?<=\\s)\\b(tch-replicant|audispd|syslog)\\b(?=\\[|:))",
            "fields": [
                {
                    "recordType": "syslog",
                    "regex": ".*(?<dstProcessId>(?<=PID\\s=\\s).*?(?=\\sLine)).*"
                }
            ]
        }
    */
    @Multiline
    public static String invalidParserConfig;
    //@formatter:on

    @Test
    public void testMalformedRegex() throws Exception {
        JSONObject parserConfig = (JSONObject) new JSONParser().parse(invalidParserConfig);
        assertThrows(IllegalStateException.class, () -> regularExpressionsParser.configure(parserConfig));
    }

    //@formatter:off
    /**
        {
            "messageHeaderRegex": "(?<syslog_priority>(?<=^<)\\d{1,4}(?=>)).*?(?<timestampDeviceOriginal>(?<=>)[A-Za-z]{3}\\s{1,2}\\d{1,2}\\s\\d{1,2}:\\d{1,2}:\\d{1,2}(?=\\s)).*?(?<deviceName>(?<=\\s).*?(?=\\s))",
            "fields": [
                {
                    "recordType": "syslog",
                    "regex": ".*(?<dstProcessId>(?<=PID\\s=\\s).*?(?=\\sLine)).*"
                }
            ]
        }
    */
    @Multiline
    public static String noRecordTypeParserConfig;
    //@formatter:on

    @Test
    public void testNoRecordTypeRegex() throws Exception {
        JSONObject parserConfig = (JSONObject) new JSONParser().parse(noRecordTypeParserConfig);
        assertThrows(IllegalStateException.class, () -> regularExpressionsParser.configure(parserConfig));
    }

    private JSONObject parse(String message) throws Exception {
        List<JSONObject> result = regularExpressionsParser.parse(message.getBytes(
            StandardCharsets.UTF_8));
        if (result.size() > 0) {
            return result.get(0);
        }
        throw new Exception("Could not parse : " + message);
    }

    @Test
    public void getsReadCharsetFromConfig() throws ParseException {
        JSONObject config = (JSONObject) new JSONParser().parse(parserConfig1);
        config.put(MessageParser.READ_CHARSET, StandardCharsets.UTF_16.toString());
        regularExpressionsParser.configure(config);
        assertThat(regularExpressionsParser.getReadCharset(), equalTo(StandardCharsets.UTF_16));
    }

    @Test
    public void getsReadCharsetFromDefault() throws ParseException {
      JSONObject config = (JSONObject) new JSONParser().parse(parserConfig1);
      regularExpressionsParser.configure(config);
      assertThat(regularExpressionsParser.getReadCharset(), equalTo(StandardCharsets.UTF_8));
    }

}
