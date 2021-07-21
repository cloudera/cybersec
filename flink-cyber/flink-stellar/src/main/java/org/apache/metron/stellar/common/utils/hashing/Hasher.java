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
package org.apache.metron.stellar.common.utils.hashing;

import org.apache.commons.codec.EncoderException;

import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public interface Hasher {

  /**
   * Returns an encoded string representation of the hash value of the input. It is expected that
   * this implementation does throw exceptions when the input is null.
   * @param toHash The value to hash.
   * @return A hash of {@code toHash} that has been encoded.
   * @throws EncoderException If unable to encode the hash then this exception occurs.
   * @throws NoSuchAlgorithmException If the supplied algorithm is not known.
   */
  Object getHash(final Object toHash) throws EncoderException, NoSuchAlgorithmException;

  /**
   * Configure the hasher with a string to object map.
   * @param config
   */
  void configure(final Optional<Map<String, Object>> config);

}
