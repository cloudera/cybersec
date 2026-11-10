package com.cloudera.cyber.ondemand.source.decode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.MessageQueryConversionError;
import lombok.NonNull;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

public class MapRowToMessage extends ProcessFunction<Row, Message> {
    public static final String SOURCE_COLUMN_NAME = "source";
    public static final String TIMESTAMP_COLUMN_NAME = "ts";
    public static final String ID_COLUMN_NAME = "id";
    public static final String FIELDS_COLUMN_NAME = "fields";
    public static Set<String> FIXED_COLUMN_NAMES = Set.of(SOURCE_COLUMN_NAME, TIMESTAMP_COLUMN_NAME, ID_COLUMN_NAME, FIELDS_COLUMN_NAME);

    public static final OutputTag<MessageQueryConversionError> MESSAGE_QUERY_CONVERSION_ERROR =
            new OutputTag<>("conversion-errors"){};
    private final HashMap<String, HashMap<String, String>> sourceToExtensionName;

    public MapRowToMessage(Map<String, MappingDto> unenrichedTopicMapping) {
        this.sourceToExtensionName  = unenrichedTopicMapping.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, e -> reverseExtensionMap(e.getValue()), (existing, replacement) -> replacement, // Merge function
                HashMap::new ));
    }

    private HashMap<String,String> reverseExtensionMap(MappingDto mappingDto) {
        return mappingDto.getColumnMapping().stream().collect(Collectors.toMap(MappingColumnDto::getName, MappingColumnDto::getProperName, (existing, replacement) -> replacement, // Merge function
                HashMap::new));
    }


    @Override
    public void processElement(Row row, ProcessFunction<Row, Message>.Context context, Collector<Message> collector) {
        try {
            String source = getRequiredField(row, SOURCE_COLUMN_NAME);
            Map<String, String> columnToExtension = sourceToExtensionName.get(source);
            if (columnToExtension != null) {
                long ts = decodeTimestamp(getRequiredField(row, TIMESTAMP_COLUMN_NAME));
                String id = getRequiredField(row, ID_COLUMN_NAME);
                Map<String, String> fields = getRequiredField(row, FIELDS_COLUMN_NAME);
                Set<String> rowFieldNames = Objects.requireNonNull(row.getFieldNames(true), "Could not get field names.");
                Map<String, String> extensions = new HashMap<>();
                for (String rowFieldName : rowFieldNames) {
                    if (!FIXED_COLUMN_NAMES.contains(rowFieldName)) {
                        String extensionName = columnToExtension.getOrDefault(rowFieldName, rowFieldName);
                        Object fieldValue = row.getField(rowFieldName);
                        if (fieldValue != null) {
                            extensions.put(extensionName, fieldValue.toString());
                        }
                    }
                }
                extensions.putAll(fields);
                Message convertedMessage = Message.builder().
                        originalSource(SignedSourceKey.builder().topic("ondemand").partition(-1).offset(-1).signature(new byte[0]).build()).
                                id(id).source(source).ts(ts).extensions(extensions).build();
               collector.collect(convertedMessage);
            } else {
                reportError(row, context, String.format("No row to message mapping defined for source '%s'", source));
            }
        } catch (RuntimeException exc) {
            reportError(row, context, exc.getMessage());
        }
    }

    private void reportError(Row row, ProcessFunction<Row, Message>.Context context, String conversionError) {
        MessageQueryConversionError error = MessageQueryConversionError.builder().
                conversionError(conversionError).rowText(row.toString()).ts(MessageUtils.getCurrentTimestamp()).build();
        context.output(MESSAGE_QUERY_CONVERSION_ERROR, error);
    }

    private Long decodeTimestamp(@NonNull Object fieldValue) {
        if (fieldValue instanceof java.time.LocalDateTime localDateTimeValue) {
            return localDateTimeValue.toInstant(java.time.ZoneOffset.UTC).toEpochMilli();
        }
        if (fieldValue instanceof Number numberValue) {
            return numberValue.longValue();
        }
        return Long.valueOf(fieldValue.toString());
    }

    private <T> T  getRequiredField(Row row, String fieldName) {
        return Objects.requireNonNull(row.getFieldAs(fieldName), String.format("Required field '%s' is not defined.", fieldName));
    }

}
