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

import com.cloudera.cyber.Message;
import org.apache.flink.types.Row;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class RowToMessageTest {

  private static final String[] EVENT_COLUMNS = {
      "id", "ts", "source", "message", "fields",
      "originalsource_topic", "originalsource_partition", "originalsource_offset",
      "originalsource_signature", "dt", "hr", "ip_dst_addr"
  };

  @Test
  void mapsFixedFieldsAndExtensionMap() {
    Map<String, String> fields = new HashMap<>();
    fields.put("dns_query", "example.com");

    LocalDateTime timestamp = LocalDateTime.of(2024, 1, 2, 3, 4, 5);
    Row row = Row.of("abc", timestamp, "squid", "a message", fields,
        "raw", 2, 99L, new byte[]{1, 2, 3}, "2024-01-02", "03", "10.0.0.1");

    Message message = new RowToMessage(EVENT_COLUMNS).map(row);

    assertThat(message.getId()).isEqualTo("abc");
    assertThat(message.getSource()).isEqualTo("squid");
    assertThat(message.getMessage()).isEqualTo("a message");
    assertThat(message.getTs()).isEqualTo(timestamp.toInstant(ZoneOffset.UTC).toEpochMilli());
    assertThat(message.getExtensions())
        .containsEntry("dns_query", "example.com")
        .containsEntry("ip_dst_addr", "10.0.0.1")
        .doesNotContainKeys("id", "ts", "source", "message", "fields", "dt", "hr");
  }

  @Test
  void rebuildsOriginalSourceFromColumns() {
    Row row = Row.of("abc", 1700000000000L, "squid", "a message", null,
        "raw", 2, 99L, new byte[]{1, 2, 3}, "2024-01-02", "03", "10.0.0.1");

    Message message = new RowToMessage(EVENT_COLUMNS).map(row);

    assertThat(message.getOriginalSource()).isNotNull();
    assertThat(message.getOriginalSource().getTopic()).isEqualTo("raw");
    assertThat(message.getOriginalSource().getPartition()).isEqualTo(2);
    assertThat(message.getOriginalSource().getOffset()).isEqualTo(99L);
    assertThat(message.getOriginalSource().getSignature()).containsExactly(1, 2, 3);
    assertThat(message.getTs()).isEqualTo(1700000000000L);
  }

  @Test
  void toleratesMissingOriginalSourceColumns() {
    String[] columns = {"id", "ts", "source", "message", "fields"};
    Row row = Row.of("abc", 1700000000000L, "squid", "a message", null);

    Message message = new RowToMessage(columns).map(row);

    assertThat(message.getOriginalSource()).isNotNull();
    assertThat(message.getOriginalSource().getTopic()).isEqualTo("squid");
    assertThat(message.getOriginalSource().getOffset()).isZero();
    assertThat(message.getId()).isEqualTo("abc");
    assertThat(message.getSource()).isEqualTo("squid");
  }

  @Test
  void producesMessageTypeInformation() {
    assertThat(new RowToMessage(EVENT_COLUMNS).getProducedType()).isNotNull();
  }
}
