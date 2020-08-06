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

package com.cloudera.parserchains.queryservice.common.utils;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.*;

import static java.lang.String.format;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.fail;

public class UniqueIDGeneratorTest {

  private Path configPath;

  @BeforeEach
  public void beforeEach() throws IOException {
    String tempDirPrefix = this.getClass().getName();
    configPath = Files.createTempDirectory(tempDirPrefix);
  }

  @Test
  public void increments_id_in_file() {
    long seed = 0;
    UniqueIDGenerator gen = new UniqueIDGenerator(configPath, seed);
    Long id = gen.incrementAndGet();
    assertThat(id, equalTo(1L));
    id = gen.incrementAndGet();
    assertThat(id, equalTo(2L));
    id = gen.incrementAndGet();
    assertThat(id, equalTo(3L));
  }

  @Test
  public void increments_id_in_file_with_multiple_threads()
      throws InterruptedException, ExecutionException, TimeoutException {
    int numThreads = 5;
    ExecutorService executorService = Executors.newFixedThreadPool(numThreads);
    long seed = 0;
    UniqueIDGenerator gen = new UniqueIDGenerator(configPath, seed);
    int numCalls = 1000;
    List<Future<?>> futures = new ArrayList<>();
    Set<Long> ids = ConcurrentHashMap.newKeySet();
    for (int i = 0; i < numCalls; i++) {
      futures.add(executorService.submit(() -> {
        Long newId = gen.incrementAndGet();
        if (!ids.add(newId)) {
          fail(format("ID %s already existed", newId));
        }
        return null;
      }));
    }
    long timeout = 1;
    for (Future<?> future : futures) {
      future.get(timeout, TimeUnit.SECONDS);
    }
    assertThat(ids.size(), equalTo(1000));
    assertThat(gen.incrementAndGet(), equalTo(1001L));
  }

}
