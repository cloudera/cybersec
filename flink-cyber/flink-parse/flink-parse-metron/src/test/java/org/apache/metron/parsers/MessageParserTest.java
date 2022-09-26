/**
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

import org.apache.metron.parsers.interfaces.MessageParser;
import org.apache.metron.parsers.interfaces.MessageParserResult;
import org.apache.metron.stellar.common.JSONMapObject;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class MessageParserTest {

  abstract class TestMessageParser implements MessageParser<JSONMapObject> {
    @Override
    public void init() {
    }

    @Override
    public boolean validate(JSONMapObject message) {
      return false;
    }

    @Override
    public void configure(Map<String, Object> config) {

    }
  }

  @Test
  public void testNullable() {
    MessageParser parser = new TestMessageParser() {
      @Override
      public List<JSONMapObject> parse(byte[] rawMessage) {
        return null;
      }
    };
    assertNotNull(parser.parseOptionalResult(null));
    assertFalse(parser.parseOptionalResult(null).isPresent());
  }

  @Test
  public void testNotNullable() {
    MessageParser<JSONMapObject> parser = new TestMessageParser() {
      @Override
      public List<JSONMapObject> parse(byte[] rawMessage) {
        return new ArrayList<>();
      }
    };
    assertNotNull(parser.parseOptionalResult(null));
    Optional<MessageParserResult<JSONMapObject>> ret = parser.parseOptionalResult(null);
    assertTrue(ret.isPresent());
    assertEquals(0, ret.get().getMessages().size());
  }

  @Test
  public void testParse() {
    JSONMapObject message = new JSONMapObject();
    MessageParser<JSONMapObject> parser = new TestMessageParser() {
      @Override
      public List<JSONMapObject> parse(byte[] rawMessage) {
        return Collections.singletonList(message);
      }
    };
    Optional<MessageParserResult<JSONMapObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    assertTrue(ret.isPresent());
    assertEquals(1, ret.get().getMessages().size());
    assertEquals(message, ret.get().getMessages().get(0));
  }

  @Test
  public void testParseOptional() {
    JSONMapObject message = new JSONMapObject();
    MessageParser<JSONMapObject> parser = new TestMessageParser() {
      @Override
      public Optional<List<JSONMapObject>> parseOptional(byte[] rawMessage) {
        return Optional.of(Collections.singletonList(message));
      }
    };
    Optional<MessageParserResult<JSONMapObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    assertTrue(ret.isPresent());
    assertEquals(1, ret.get().getMessages().size());
    assertEquals(message, ret.get().getMessages().get(0));
  }

  @Test
  public void testParseException() {
    MessageParser<JSONMapObject> parser = new TestMessageParser() {
      @Override
      public List<JSONMapObject> parse(byte[] rawMessage) {
        throw new RuntimeException("parse exception");
      }
    };
    Optional<MessageParserResult<JSONMapObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    assertTrue(ret.isPresent());
    assertTrue(ret.get().getMasterThrowable().isPresent());
    assertEquals("parse exception", ret.get().getMasterThrowable().get().getMessage());
  }

  @Test
  public void testParseOptionalException() {
    MessageParser<JSONMapObject> parser = new TestMessageParser() {
      @Override
      public Optional<List<JSONMapObject>> parseOptional(byte[] rawMessage) {
        throw new RuntimeException("parse exception");
      }
    };
    Optional<MessageParserResult<JSONMapObject>> ret = parser.parseOptionalResult("message".getBytes(
        StandardCharsets.UTF_8));
    assertTrue(ret.isPresent());
    assertTrue(ret.get().getMasterThrowable().isPresent());
    assertEquals("parse exception", ret.get().getMasterThrowable().get().getMessage());
  }

}
