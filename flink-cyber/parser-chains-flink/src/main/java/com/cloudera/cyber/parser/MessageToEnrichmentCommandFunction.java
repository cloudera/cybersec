package com.cloudera.cyber.parser;

import com.cloudera.cyber.*;
import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.streaming.api.functions.ProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.io.Serializable;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
public class MessageToEnrichmentCommandFunction extends ProcessFunction<Message, EnrichmentCommand> {

    public static final String STREAMING_ENRICHMENT_FEATURE = "stream_enrich";
    public static final String STREAMING_ENRICHMENT_KEY_FIELD_NOT_SET = "Message does not define values for key field for enrichment type '%s'";
    public static final String STREAMING_ENRICHMENT_VALUE_FIELD_NOT_SET = "Message does not contain any values for enrichment type '%s'";
    public static final String VALUE_FIELDS = "valueFields";
    private final OutputTag<Message> errorOutputTag = new OutputTag<Message>(ParserJob.ERROR_MESSAGE_SIDE_OUTPUT){};

    @Data
    @AllArgsConstructor
    private static class EnrichmentTypeConfig implements Serializable {
        private String enrichmentType;
        private EnrichmentFieldsConfig fieldsConfig;
    }

    private final HashMap<String, ArrayList<EnrichmentTypeConfig>> sourceToEnrichmentsConfig = new HashMap<>();


    public MessageToEnrichmentCommandFunction(List<String> streamingEnrichmentSources, EnrichmentsConfig streamingEnrichmentConfig) {
        streamingEnrichmentSources.forEach(source -> sourceToEnrichmentsConfig.put(source, new ArrayList<>()));
        streamingEnrichmentConfig.getEnrichmentConfigs().forEach((key, value) -> {
            List<String> configSources = value.getFieldMapping().getStreamingSources();
            if (configSources != null) {
                for (String configSource : configSources) {
                    List<EnrichmentTypeConfig> configList = sourceToEnrichmentsConfig.get(configSource);
                    if (configList != null) {
                        configList.add(new EnrichmentTypeConfig(key, value.getFieldMapping()));
                    }
                }
            }
        });
    }

    @Override
    public void processElement(Message message, Context context, Collector<EnrichmentCommand> collector) {
        CommandType commandType = CommandType.ADD;
        Map<String, String> extensions = message.getExtensions();
        for (EnrichmentTypeConfig typeConfig : sourceToEnrichmentsConfig.get(message.getSource())) {
            EnrichmentFieldsConfig fieldsConfig = typeConfig.getFieldsConfig();
            String enrichmentType = typeConfig.getEnrichmentType();

            List<String> keyFieldValues = new ArrayList<>();
            List<DataQualityMessage> dataQualityMessages = new ArrayList<>();
            Map<String, String> valueFieldValues = null;

            extractKeyFields(typeConfig.getEnrichmentType(), fieldsConfig, message.getExtensions(), keyFieldValues, dataQualityMessages);
            if (dataQualityMessages.isEmpty() && extensions != null) {
                valueFieldValues = extractValueFields(enrichmentType, fieldsConfig, extensions, dataQualityMessages);
            }
            if (dataQualityMessages.isEmpty()) {
                String enrichmentKey = keyFieldValues.stream().collect(Collectors.joining(fieldsConfig.getKeyDelimiter()));
                EnrichmentEntry enrichmentEntry = EnrichmentEntry.builder().ts(message.getTs()).
                        type(typeConfig.getEnrichmentType()).
                        key(enrichmentKey).entries(valueFieldValues).build();
                log.info("Writing enrichment key {}", enrichmentKey);
                collector.collect(EnrichmentCommand.builder().
                        headers(Collections.emptyMap()).
                        type(commandType).payload(enrichmentEntry).build());
            }
            else {
                context.output(errorOutputTag, MessageUtils.enrich(message, Collections.emptyMap(), dataQualityMessages));
            }
        }
    }

    private void extractKeyFields(String enrichmentType, EnrichmentFieldsConfig fieldsConfig, Map<String, String> extensions,
                                          List<String> keyFieldValues, List<DataQualityMessage> dataQualityMessages) {
        for (String keyFieldName : fieldsConfig.getKeyFields()) {
            String keyFieldValue = extensions != null ? extensions.get(keyFieldName) : null;
            if (keyFieldValue != null) {
                keyFieldValues.add(keyFieldValue);
            } else {
                dataQualityMessages.add(DataQualityMessage.builder().
                        level(DataQualityMessageLevel.ERROR.name()).
                        feature(STREAMING_ENRICHMENT_FEATURE).
                        field(keyFieldName).
                        message(String.format(STREAMING_ENRICHMENT_KEY_FIELD_NOT_SET, enrichmentType)).
                        build());
            }
        }
    }

    private Map<String, String> extractValueFields(String enrichmentType, EnrichmentFieldsConfig fieldsConfig, Map<String, String> extensions,
                                    List<DataQualityMessage> dataQualityMessages) {
        List<String>  keyFieldNames = fieldsConfig.getKeyFields();
        Map<String, String> valueFieldValues;
        List<String> valueFieldNames = fieldsConfig.getValueFields();

        if (valueFieldNames == null) {
            // no fields specified, write all the extensions minus the key fields
            valueFieldValues = extensions.entrySet().stream().filter(e -> !keyFieldNames.contains(e.getKey())).
                    collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        } else {
            valueFieldValues = valueFieldNames.stream().map(fieldValueName -> new AbstractMap.SimpleEntry<>(fieldValueName, extensions.get(fieldValueName))).
                    filter(e -> (e.getValue() != null)).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        }
        if (valueFieldValues.isEmpty()) {
            dataQualityMessages.add(DataQualityMessage.builder().
                    level(DataQualityMessageLevel.ERROR.name()).
                    feature(STREAMING_ENRICHMENT_FEATURE).
                    field(VALUE_FIELDS).
                    message(String.format(STREAMING_ENRICHMENT_VALUE_FIELD_NOT_SET, enrichmentType)).
                    build());
        }
        return valueFieldValues;
    }
}
