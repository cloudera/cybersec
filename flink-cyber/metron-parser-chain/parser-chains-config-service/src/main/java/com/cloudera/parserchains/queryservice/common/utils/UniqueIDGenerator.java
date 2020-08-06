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

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.List;

public class UniqueIDGenerator implements IDGenerator<Long> {

  public static final String ID_FILENAME = "idgenerator";
  private final long seed;
  private final Path idFile;
  private final Path idGenSourceDir;

  public UniqueIDGenerator(Path idGenSourceDir) {
    this(idGenSourceDir, 0L);
  }

  public UniqueIDGenerator(Path idGenSourceDir, long seed) {
    this.idGenSourceDir = idGenSourceDir;
    this.idFile = idGenSourceDir.resolve(Paths.get(ID_FILENAME));
    this.seed = seed;
  }

  @Override
  public Long incrementAndGet() {
    synchronized (UniqueIDGenerator.class) {
      if (Files.exists(idFile)) {
        try {
          List<String> lines = Files.readAllLines(idFile);
          long id = Long.parseLong(lines.get(0));
          Files.write(idFile, Long.toString(++id).getBytes(), StandardOpenOption.TRUNCATE_EXISTING);
          return id;
        } catch (IOException e) {
          throw new RuntimeException("Unable to find and increment id", e);
        }
      } else {
        try {
          long id = seed;
          Files.write(idFile, Long.toString(++id).getBytes());
          return id;
        } catch (IOException e) {
          throw new RuntimeException("Unable to find and increment id", e);
        }
      }
    }
  }
}
