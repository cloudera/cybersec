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

package com.cloudera.cyber.enrichment.ondemand;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Builds the SQL used to read the log events to re-enrich.
 *
 * <p>The query selects every column of the log events table between two epoch millisecond timestamps and
 * restricts the result to the requested source values.  The timestamp column is compared against
 * {@code TO_TIMESTAMP_LTZ(<epoch millis>, 3)}, matching the way the Hive indexer writes the {@code ts}
 * column.
 */
public final class LogEventQuery {

  public static final String DEFAULT_TS_COLUMN = "ts";
  public static final String DEFAULT_SOURCE_COLUMN = "source";

  private LogEventQuery() {
  }

  public static String buildSelectSql(String tableName, String tsColumn, String sourceColumn,
                                      long startTs, long endTs, List<String> sources) {
    if (tableName == null || tableName.trim().isEmpty()) {
      throw new IllegalArgumentException("A log events table name must be provided");
    }
    if (sources == null || sources.isEmpty()) {
      throw new IllegalArgumentException("At least one source must be provided");
    }
    if (startTs > endTs) {
      throw new IllegalArgumentException(
          String.format("The start timestamp [%d] must not be after the end timestamp [%d]", startTs, endTs));
    }

    String sourceList = sources.stream()
        .filter(source -> source != null && !source.trim().isEmpty())
        .map(source -> String.format("'%s'", source.replace("'", "''")))
        .collect(Collectors.joining(", "));

    if (sourceList.isEmpty()) {
      throw new IllegalArgumentException("At least one non empty source must be provided");
    }

    return String.join(" ",
        "SELECT * FROM", tableName,
        String.format("WHERE `%s` >= TO_TIMESTAMP_LTZ(%d, 3)", tsColumn, startTs),
        String.format("AND `%s` <= TO_TIMESTAMP_LTZ(%d, 3)", tsColumn, endTs),
        String.format("AND `%s` IN (%s)", sourceColumn, sourceList));
  }
}
