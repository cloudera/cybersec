/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package org.apache.metron.enrichment.adapters.maxmind;/*
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

import java.lang.invoke.MethodHandles;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public interface MaxMindDatabase {
  Logger LOG = LoggerFactory.getLogger(MethodHandles.lookup().lookupClass());
  String EXTENSION_TAR_GZ = ".tar.gz";
  String EXTENSION_MMDB_GZ = ".mmdb.gz";

  /**
   * Retrieves the configuration key that holds the HDFS database file location
   * @return The configuration key
   */
  String getHdfsFileConfig();

  /**
   * Retrieves the default HDFS database file location
   * @return The HDFS database file location
   */
  String getHdfsFileDefault();

  /**
   * Locks any underlying resources to ensure they are smoothly updated without disruption.
   * Any callers implementing an update() function should lock during the update to ensure uninterrupted querying.
   */
  void lockIfNecessary();

  /**
   * Unlocks any underlying resources to ensure the lock is released after an update.
   * Any callers implementing an update() function should ensure they've unlocked post update.
   */
  void unlockIfNecessary();

  /**
   * Updates the database file, if the configuration points to a new file.
   * Implementations may need to be synchronized to avoid issues querying during updates.
   *
   * @param globalConfig The global configuration that will be used to determine if an update is necessary.
   */
  void updateIfNecessary(Map<String, Object> globalConfig);

  void readDatabaseContents(String hdfsFile);
  /**
   * Update the database being queried to one backed by the provided HDFS file.
   * Access to the database should be guarded by read locks to avoid disruption while updates are occurring.
   * @param hdfsFile The HDFS file path to be used for new queries.
   */
  default void update(String hdfsFile) {
    // If nothing is set (or it's been unset, use the defaults)
    if (hdfsFile == null || hdfsFile.isEmpty()) {
      LOG.debug("Using default for {}: {}", getHdfsFileConfig(), getHdfsFileDefault());
      hdfsFile = getHdfsFileDefault();
    }

    lockIfNecessary();
    try {
      readDatabaseContents(hdfsFile);
    } finally {
      unlockIfNecessary();
    }
  }
}
