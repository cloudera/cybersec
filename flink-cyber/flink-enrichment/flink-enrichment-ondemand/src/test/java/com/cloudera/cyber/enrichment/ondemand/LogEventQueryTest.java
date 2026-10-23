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

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class LogEventQueryTest {

  @Test
  void buildsSelectWithTimestampRangeAndSources() {
    String sql = LogEventQuery.buildSelectSql("cyber.events", "ts", "source",
        1000L, 2000L, Arrays.asList("squid", "dns"));

    assertThat(sql).isEqualTo(
        "SELECT * FROM cyber.events "
            + "WHERE `ts` >= TO_TIMESTAMP_LTZ(1000, 3) "
            + "AND `ts` <= TO_TIMESTAMP_LTZ(2000, 3) "
            + "AND `source` IN ('squid', 'dns')");
  }

  @Test
  void escapesSingleQuotesInSources() {
    String sql = LogEventQuery.buildSelectSql("events", "ts", "source",
        0L, 1L, Collections.singletonList("o'brien"));

    assertThat(sql).contains("`source` IN ('o''brien')");
  }

  @Test
  void supportsASingleSource() {
    String sql = LogEventQuery.buildSelectSql("events", "ts", "source",
        0L, 1L, Collections.singletonList("squid"));

    assertThat(sql).contains("`source` IN ('squid')");
  }

  @Test
  void rejectsEmptySourceList() {
    assertThatThrownBy(() -> LogEventQuery.buildSelectSql("events", "ts", "source",
        0L, 1L, Collections.emptyList()))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("At least one source");
  }

  @Test
  void rejectsStartAfterEnd() {
    assertThatThrownBy(() -> LogEventQuery.buildSelectSql("events", "ts", "source",
        2000L, 1000L, Collections.singletonList("squid")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("must not be after");
  }

  @Test
  void rejectsMissingTableName() {
    assertThatThrownBy(() -> LogEventQuery.buildSelectSql(" ", "ts", "source",
        0L, 1L, Collections.singletonList("squid")))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("table name");
  }
}
