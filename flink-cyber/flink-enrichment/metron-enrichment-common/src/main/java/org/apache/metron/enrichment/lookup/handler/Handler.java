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
package org.apache.metron.enrichment.lookup.handler;

import org.apache.metron.enrichment.lookup.LookupKey;

import java.io.IOException;

public interface Handler<CONTEXT_T, KEY_T extends LookupKey, RESULT_T> extends AutoCloseable{
  boolean exists(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException;
  RESULT_T get(KEY_T key, CONTEXT_T context, boolean logAccess) throws IOException;
  Iterable<Boolean> exists(Iterable<KeyWithContext<KEY_T, CONTEXT_T>> key, boolean logAccess) throws IOException;
  Iterable<RESULT_T> get(Iterable<KeyWithContext<KEY_T, CONTEXT_T>> key, boolean logAccess) throws IOException;
}
