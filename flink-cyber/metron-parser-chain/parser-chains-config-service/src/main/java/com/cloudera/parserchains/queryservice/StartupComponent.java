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

package com.cloudera.parserchains.queryservice;

import com.cloudera.parserchains.queryservice.config.AppProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * This class is being used to do initialization for the REST application.
 * e.g. creating folder hierarchies needed by the application.
 */
@Component
@Slf4j
public class StartupComponent implements CommandLineRunner {

  private final AppProperties appProperties;

  public StartupComponent(AppProperties appProperties) {
    this.appProperties = appProperties;
  }

  @Override
  public void run(String... args) throws Exception {
    log.info("Running startup stuff");
    log.info("Creating the config dir: {}", appProperties.getConfigPath());
    Files.createDirectories(Paths.get(appProperties.getConfigPath()));
    log.info("Done creating the config dir");
  }
}
