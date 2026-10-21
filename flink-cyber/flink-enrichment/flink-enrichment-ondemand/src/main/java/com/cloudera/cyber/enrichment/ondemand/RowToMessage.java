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
import com.cloudera.cyber.SignedSourceKey;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.ResultTypeQueryable;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
import org.apache.flink.types.Row;

import java.nio.ByteBuffer;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

/**
 * Maps a Hive log events table row back to a {@link Message}.
 *
 * <p>Columns are located by name, so the order produced by the read query does not matter.  The
 * {@code fields} map column is copied into the message extensions, then every remaining column that is
 * not one of the well known message columns is added to the extensions as a string.  The original source
 * coordinates are rebuilt from the {@code originalsource_*} columns when the topic column is present.
 */
public class RowToMessage extends RichMapFunction<Row, Message> implements ResultTypeQueryable<Message> {

  public static final String COLUMN_ID = "id";
  public static final String COLUMN_TS = "ts";
  public static final String COLUMN_SOURCE = "source";
  public static final String COLUMN_MESSAGE = "message";
  public static final String COLUMN_FIELDS = "fields";
  public static final String COLUMN_ORIGINAL_SOURCE_TOPIC = "originalsource_topic";
  public static final String COLUMN_ORIGINAL_SOURCE_PARTITION = "originalsource_partition";
  public static final String COLUMN_ORIGINAL_SOURCE_OFFSET = "originalsource_offset";
  public static final String COLUMN_ORIGINAL_SOURCE_SIGNATURE = "originalsource_signature";
  public static final String COLUMN_PARTITION_DATE = "dt";
  public static final String COLUMN_PARTITION_HOUR = "hr";

  private static final String[] RESERVED_COLUMNS = {
      COLUMN_ID, COLUMN_TS, COLUMN_SOURCE, COLUMN_MESSAGE, COLUMN_FIELDS,
      COLUMN_ORIGINAL_SOURCE_TOPIC, COLUMN_ORIGINAL_SOURCE_PARTITION,
      COLUMN_ORIGINAL_SOURCE_OFFSET, COLUMN_ORIGINAL_SOURCE_SIGNATURE,
      COLUMN_PARTITION_DATE, COLUMN_PARTITION_HOUR
  };

  private final String[] fieldNames;

  public RowToMessage(String[] fieldNames) {
    this.fieldNames = fieldNames == null ? new String[0] : fieldNames;
  }

  @Override
  public Message map(Row row) {
    Map<String, String> extensions = new HashMap<>();
    copyExtensionMap(row, findColumn(COLUMN_FIELDS), extensions);
    copyExtraColumns(row, extensions);

    String id = valueAsString(row, findColumn(COLUMN_ID));
    Long ts = valueAsEpochMillis(row, findColumn(COLUMN_TS));
    String source = valueAsString(row, findColumn(COLUMN_SOURCE));
    String message = valueAsString(row, findColumn(COLUMN_MESSAGE));

    return Message.builder()
        .id(id == null ? UUID.randomUUID().toString() : id)
        .ts(ts == null ? 0L : ts)
        .source(source)
        .message(message == null ? "" : message)
        .extensions(extensions)
        .originalSource(buildOriginalSource(row, source))
        .build();
  }

  @Override
  public TypeInformation<Message> getProducedType() {
    return new AvroTypeInfo<>(Message.class);
  }

  private int findColumn(String name) {
    for (int i = 0; i < fieldNames.length; i++) {
      if (name.equalsIgnoreCase(fieldNames[i])) {
        return i;
      }
    }
    return -1;
  }

  private SignedSourceKey buildOriginalSource(Row row, String source) {
    int topicIndex = findColumn(COLUMN_ORIGINAL_SOURCE_TOPIC);
    if (topicIndex < 0) {
      // Message.originalSource is mandatory, so fall back to the message source when the log events table
      // does not carry the original kafka coordinates.
      return SignedSourceKey.builder()
          .topic(source)
          .partition(0)
          .offset(0L)
          .signature(new byte[0])
          .build();
    }
    return SignedSourceKey.builder()
        .topic(valueAsString(row, topicIndex))
        .partition(valueAsInt(row, findColumn(COLUMN_ORIGINAL_SOURCE_PARTITION)))
        .offset(valueAsLong(row, findColumn(COLUMN_ORIGINAL_SOURCE_OFFSET)))
        .signature(valueAsBytes(row, findColumn(COLUMN_ORIGINAL_SOURCE_SIGNATURE)))
        .build();
  }

  private void copyExtraColumns(Row row, Map<String, String> extensions) {
    for (int i = 0; i < row.getArity() && i < fieldNames.length; i++) {
      String name = fieldNames[i];
      if (name == null || isReservedColumn(name)) {
        continue;
      }
      Object value = row.getField(i);
      if (value != null) {
        extensions.put(name, value.toString());
      }
    }
  }

  private static boolean isReservedColumn(String name) {
    for (String reserved : RESERVED_COLUMNS) {
      if (reserved.equalsIgnoreCase(name)) {
        return true;
      }
    }
    return false;
  }

  @SuppressWarnings("unchecked")
  private void copyExtensionMap(Row row, int index, Map<String, String> extensions) {
    if (index < 0) {
      return;
    }
    Object value = row.getField(index);
    if (value instanceof Map) {
      ((Map<Object, Object>) value).forEach((k, v) -> {
        if (k != null && v != null) {
          extensions.put(k.toString(), v.toString());
        }
      });
    }
  }

  private static String valueAsString(Row row, int index) {
    Object value = index < 0 || index >= row.getArity() ? null : row.getField(index);
    return value == null ? null : value.toString();
  }

  private static Long valueAsLong(Row row, int index) {
    Object value = index < 0 || index >= row.getArity() ? null : row.getField(index);
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return value == null ? null : Long.valueOf(value.toString());
  }

  /**
   * The Hive table stores the message timestamp as a SQL timestamp, which the table api materialises as
   * a {@link java.time.LocalDateTime}.  Kafka based messages use epoch milliseconds, so both are accepted.
   */
  private static Long valueAsEpochMillis(Row row, int index) {
    Object value = index < 0 || index >= row.getArity() ? null : row.getField(index);
    if (value instanceof java.time.LocalDateTime) {
      return ((java.time.LocalDateTime) value).toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
    }
    if (value instanceof Number) {
      return ((Number) value).longValue();
    }
    return value == null ? null : Long.valueOf(value.toString());
  }

  private static int valueAsInt(Row row, int index) {
    Long value = valueAsLong(row, index);
    return value == null ? 0 : value.intValue();
  }

  private static byte[] valueAsBytes(Row row, int index) {
    Object value = index < 0 || index >= row.getArity() ? null : row.getField(index);
    if (value instanceof byte[]) {
      return (byte[]) value;
    }
    if (value instanceof ByteBuffer) {
      ByteBuffer buffer = (ByteBuffer) value;
      byte[] bytes = new byte[buffer.remaining()];
      buffer.get(bytes);
      return bytes;
    }
    return null;
  }
}
