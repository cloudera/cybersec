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

package org.apache.metron.common.utils;

import org.apache.commons.lang3.tuple.Pair;

import java.util.Optional;
import java.util.function.Function;

import static java.lang.String.format;

public enum RuntimeErrors {
  ILLEGAL_ARG(t -> new IllegalArgumentException(formatReason(t), t.getRight().orElse(null))),
  ILLEGAL_STATE(t -> new IllegalStateException(formatReason(t), t.getRight().orElse(null)));

  Function<Pair<String, Optional<Throwable>>, RuntimeException> func;

  RuntimeErrors(Function<Pair<String, Optional<Throwable>>, RuntimeException> func) {
    this.func = func;
  }

  /**
   * Throw runtime exception with "reason".
   *
   * @param reason Message to include in exception
   */
  public void throwRuntime(String reason) {
    throwRuntime(reason, Optional.empty());
  }

  /**
   * Throw runtime exception with format "reason + cause message + cause Throwable".
   *
   * @param reason Message to include in exception
   * @param t Wrapped exception
   */
  public void throwRuntime(String reason, Throwable t) {
    throwRuntime(reason, Optional.of(t));
  }

  /**
   * Throw runtime exception with format "reason + cause message + cause Throwable".
   * If the optional Throwable is empty/null, the exception will only include "reason".
   *
   * @param reason Message to include in exception
   * @param t Optional wrapped exception
   */
  public void throwRuntime(String reason, Optional<Throwable> t) {
    throw func.apply(Pair.of(reason, t));
  }

  private static String formatReason(Pair<String, Optional<Throwable>> p) {
    return formatReason(p.getLeft(), p.getRight());
  }

  private static String formatReason(String reason, Optional<Throwable> t) {
    if (t.isPresent()) {
      return format("%s - reason:%s", reason, t.get());
    } else {
      return format("%s", reason);
    }
  }
}
