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
package org.apache.metron.parsers.interfaces;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Optional;
import org.apache.commons.lang3.NotImplementedException;
import org.apache.metron.parsers.DefaultMessageParserResult;

public interface MessageParser<T> extends Configurable {

  String READ_CHARSET = "readCharset"; // property to use for getting the read charset from parser config

  /**
   * Initialize the message parser.  This is done once.
   */
  void init();

  /**
   * Take raw data and convert it to a list of messages.
   *
   * @param rawMessage the raw bytes of the message
   * @return If null is returned, this is treated as an empty list.
   */
  @Deprecated
  default List<T> parse(byte[] rawMessage) {
    throw new NotImplementedException("parse is not implemented");
  }

  /**
   * Take raw data and convert it to an optional list of messages.
   * @param parseMessage the raw bytes of the message
   * @return If null is returned, this is treated as an empty list.
   */
  @Deprecated
  default Optional<List<T>> parseOptional(byte[] parseMessage) {
    return Optional.ofNullable(parse(parseMessage));
  }

  /**
   * Take raw data and convert it to messages.  Each raw message may produce multiple messages and therefore
   * multiple errors.  A {@link MessageParserResult} is returned, which will have both the messages produced
   * and the errors.
   * @param parseMessage the raw bytes of the message
   * @return Optional of {@link MessageParserResult}
   */
  default Optional<MessageParserResult<T>> parseOptionalResult(byte[] parseMessage) {
    Optional<MessageParserResult<T>> result = Optional.empty();
    try {
      Optional<List<T>> optionalMessages = parseOptional(parseMessage);
      if (optionalMessages.isPresent()) {
        result = Optional.of(new DefaultMessageParserResult<>(optionalMessages.get()));
      }
    } catch (Throwable t) {
      return Optional.of(new DefaultMessageParserResult<>(t));
    }
    return result;
  }

  /**
   * Validate the message to ensure that it's correct.
   * @param message the message to validate
   * @return true if the message is valid, false if not
   */
  boolean validate(T message);

  /**
   * Provides a hook to override the default charset parsers use to read data.
   * @return Charset to use for for reading
   */
  default Charset getReadCharset() {
    return StandardCharsets.UTF_8;
  }
}
