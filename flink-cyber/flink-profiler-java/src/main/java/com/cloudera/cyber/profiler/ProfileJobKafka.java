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

package com.cloudera.cyber.profiler;

import com.cloudera.cyber.ValidateUtils;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat;
import com.cloudera.cyber.flink.ConfigConstants;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.jdbc.connector.jdbc.JdbcConnectionOptions;
import com.cloudera.cyber.jdbc.connector.jdbc.JdbcExecutionOptions;
import com.cloudera.cyber.jdbc.connector.jdbc.JdbcSink;
import com.cloudera.cyber.jdbc.connector.jdbc.internal.JdbcStatementBuilder;
import com.cloudera.cyber.profiler.dto.MeasurementDataDto;
import com.cloudera.cyber.profiler.dto.ProfileDto;
import com.cloudera.cyber.profiler.phoenix.PhoenixAuthenticationType;
import com.cloudera.cyber.profiler.phoenix.PhoenixThinClient;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import com.google.common.collect.Iterables;
import com.google.common.collect.Maps;
import freemarker.template.TemplateException;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.hbase.sink.HBaseSinkFunction;
import org.apache.flink.connector.kafka.sink.KafkaSink;
import org.apache.flink.connector.kafka.source.KafkaSource;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.sink.SinkFunction;
import org.apache.flink.util.Preconditions;

import java.io.IOException;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Consumer;

import static com.cloudera.cyber.flink.ConfigConstants.PARAMS_TOPIC_INPUT;
import static com.cloudera.cyber.profiler.phoenix.PhoenixThinClient.*;

public class ProfileJobKafka extends ProfileJob {

    private static final String PROFILE_GROUP_ID = "profile";
    private static final String PARAMS_FIRST_SEEN_HBASE_TABLE = "profile.first.seen.table";
    private static final String PARAMS_FIRST_SEEN_HBASE_COLUMN_FAMILY = "profile.first.seen.column.family";
    private static final String PARAMS_FIRST_SEEN_HBASE_FORMAT = "profile.first.seen.format";
    private static final String PARAMS_PHOENIX_DB_BATCH_SIZE = "phoenix.db.batchSize";

    private static final String PARAMS_PHOENIX_DB_INTERVAL_MILLIS = "phoenix.db.interval_millis";

    private static final String PARAMS_PHOENIX_DB_MAX_RETRY_TIMES = "phoenix.db.max_retries_times";
    private static final String UPSERT_SQL = "<#assign key_count = field_key_count?number >UPSERT INTO ${measurement_data_table_name} (MEASUREMENT_ID, <#list 1..key_count as i> KEY_${i},</#list> MEASUREMENT_NAME, MEASUREMENT_TYPE, MEASUREMENT_TIME, MEASUREMENT_VALUE) VALUES(?, <#list 1..key_count as i> ?,</#list> ?, ?, ?, ?) ON DUPLICATE KEY IGNORE";
    private static final String PARAMS_GROUP_ID = "kafka.group.id";

    private static final String EMPTY_ERROR_MESSAGE_TEMPLATE = "'%s' can not be empty.";

    private static final String INCORRECT_NUMERIC_MESSAGE_TEMPLATE = "Property '%s' has incorrect value '%s'. It should be numeric";

    private KafkaSink<ScoredMessage> sink;

    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);
        validatePhoenixParam(params);
        FlinkUtils.executeEnv(new ProfileJobKafka()
                .createPipeline(params), "Flink Profiling", params);
    }

    @Override
    protected DataStream<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params) {
        return env.fromSource(
                new FlinkUtils<>(ScoredMessage.class).createKafkaGenericSource(params.getRequired(PARAMS_TOPIC_INPUT), params, params.get(PARAMS_GROUP_ID, PROFILE_GROUP_ID)),
                WatermarkStrategy.noWatermarks(), "Kafka Source").uid("kafka-source");
    }

    @Override
    protected void writeResults(ParameterTool params, DataStream<ScoredMessage> results) {
        if (sink == null) {
            sink = new FlinkUtils<>(ScoredMessage.class).createKafkaSink(
                    params.getRequired(ConfigConstants.PARAMS_TOPIC_OUTPUT), PROFILE_GROUP_ID,
                    params);
        }
        results.sinkTo(sink).name("Kafka Results").uid("kafka.results.");
    }

    protected void writeProfileMeasurementsResults(ParameterTool params, List<ProfileDto> profileDtos, DataStream<ProfileMessage> results) throws IOException, TemplateException {
        if (params.getBoolean(PARAMS_PHOENIX_DB_INIT)) {
            DataStream<MeasurementDataDto> measurementDtoDataStream = results.flatMap(new ProfileMessageToMeasurementDataDtoMapping(new ArrayList<>(profileDtos)));
            Properties properties = Utils.readProperties(params.getProperties(), PARAMS_PHOENIX_DB_QUERY_PARAM);
            JdbcStatementBuilder<MeasurementDataDto> objectJdbcStatementBuilder = new PhoenixJdbcStatementBuilder(params.getInt(PARAMS_PHOENIX_DB_QUERY_KEY_COUNT, 0));
            JdbcExecutionOptions.Builder jdbcExecutionOptionsBuilder = JdbcExecutionOptions.builder();
            validateNumericParamAndApply(PARAMS_PHOENIX_DB_BATCH_SIZE, params.get(PARAMS_PHOENIX_DB_BATCH_SIZE), jdbcExecutionOptionsBuilder::withBatchSize);
            validateNumericParamAndApply(PARAMS_PHOENIX_DB_INTERVAL_MILLIS, params.get(PARAMS_PHOENIX_DB_INTERVAL_MILLIS), jdbcExecutionOptionsBuilder::withBatchIntervalMs);
            validateNumericParamAndApply(PARAMS_PHOENIX_DB_MAX_RETRY_TIMES, params.get(PARAMS_PHOENIX_DB_MAX_RETRY_TIMES), jdbcExecutionOptionsBuilder::withMaxRetries);

            JdbcExecutionOptions executionOptions = jdbcExecutionOptionsBuilder.build();
            JdbcConnectionOptions connectionOptions = new JdbcConnectionOptions.JdbcConnectionOptionsBuilder()
                    .withDriverName(PhoenixThinClient.getDRIVER())
                    .withUrl(client.getDbUrl())
                    .build();
            SinkFunction<MeasurementDataDto> jdbcSink = JdbcSink.sink(
                    freemarkerGenerator.replaceByTemplate(UPSERT_SQL, Maps.fromProperties(properties)),
                    objectJdbcStatementBuilder,
                    executionOptions,
                    connectionOptions);
            measurementDtoDataStream.addSink(jdbcSink).name("JDBC Sink").uid("jdbc.profile.group");
        }
    }

    @Override
    protected DataStream<ProfileMessage> updateFirstSeen(ParameterTool params, DataStream<ProfileMessage> results,
                                                         ProfileGroupConfig profileGroupConfig) {
        String tableName = params.getRequired(PARAMS_FIRST_SEEN_HBASE_TABLE);
        String columnFamilyName = params.getRequired(PARAMS_FIRST_SEEN_HBASE_COLUMN_FAMILY);
        String storageFormatString = params.get(PARAMS_FIRST_SEEN_HBASE_FORMAT, EnrichmentStorageFormat.HBASE_METRON.name());
        EnrichmentStorageFormat storageFormat = EnrichmentStorageFormat.valueOf(storageFormatString);
        EnrichmentStorageConfig enrichmentStorageConfig = new EnrichmentStorageConfig(storageFormat, tableName, columnFamilyName);
        // look up the previous first seen timestamp and update the profile message
        DataStream<ProfileMessage> updatedProfileMessages = results
                .map(new FirstSeenHbaseLookup(enrichmentStorageConfig, profileGroupConfig));

        // write the new first and last seen timestamps in hbase
        HBaseSinkFunction<ProfileMessage> hbaseSink = new FirstSeenHbaseSink(enrichmentStorageConfig, profileGroupConfig,
                params);
        updatedProfileMessages.addSink(hbaseSink).name("HBase First Seen Profile Sink");
        return updatedProfileMessages;
    }

    @Override
    protected DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params) {
        String topic = params.getRequired("query.input.topic");
        KafkaSource<ScoringRuleCommand> source = new FlinkUtils<>(ScoringRuleCommand.class).createKafkaGenericSource(topic, params, params.get(PARAMS_GROUP_ID, PROFILE_GROUP_ID));
        return env.fromSource(source, WatermarkStrategy.noWatermarks(), "Kafka Score Rule Source")
                .uid("kafka.input.rule.command");
    }

    @Override
    protected void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results) {
        String topic = params.getRequired("query.output.topic");
        KafkaSink<ScoringRuleCommandResult> scoredSink = new FlinkUtils<>(ScoringRuleCommandResult.class).createKafkaSink(topic, params.get(PARAMS_GROUP_ID, PROFILE_GROUP_ID), params);
        results.sinkTo(scoredSink).name("Kafka Score Rule Command Results").uid("kafka.output.rule.command.results");

    }

    private static class PhoenixJdbcStatementBuilder implements JdbcStatementBuilder<MeasurementDataDto> {
        private final int keyCount;

        PhoenixJdbcStatementBuilder(int keyCount) {
            this.keyCount = keyCount;
        }

        @Override
        public void accept(PreparedStatement ps, MeasurementDataDto measurementDataDto) throws SQLException {
            int index = 1;
            ps.setInt(index++, measurementDataDto.getMeasurementId());
            for (int i = 0; i < keyCount; i++) {
                ps.setString(index++, Iterables.get(Optional.ofNullable(measurementDataDto.getKeys()).orElseGet(ArrayList::new), i, null));
            }
            ps.setString(index++, measurementDataDto.getMeasurementName());
            ps.setString(index++, measurementDataDto.getMeasurementType());
            ps.setTimestamp(index++, measurementDataDto.getMeasurementTime());
            ps.setDouble(index, measurementDataDto.getMeasurementValue());
        }
    }

    private void validateNumericParamAndApply(String paramName, String paramValue, Consumer<Integer> consumer) {
        Preconditions.checkArgument(StringUtils.isNumeric(paramValue), INCORRECT_NUMERIC_MESSAGE_TEMPLATE, paramName, paramValue);
        consumer.accept(Integer.parseInt(paramValue));
    }

    private static void validatePhoenixParam(ParameterTool params) {
        String phoenixFlag = params.get(PARAMS_PHOENIX_DB_INIT);
        if (Boolean.parseBoolean(phoenixFlag)) {
            Preconditions.checkArgument(StringUtils.isNotEmpty(params.get(PHOENIX_THIN_PROPERTY_URL)), EMPTY_ERROR_MESSAGE_TEMPLATE, PHOENIX_THIN_PROPERTY_URL);
            if (!PhoenixAuthenticationType.SPNEGO.name().equalsIgnoreCase(params.get(PHOENIX_THIN_PROPERTY_AUTHENTICATION, ""))) {
                Preconditions.checkArgument(StringUtils.isNotEmpty(params.get(PHOENIX_THIN_PROPERTY_AVATICA_USER)), EMPTY_ERROR_MESSAGE_TEMPLATE, PHOENIX_THIN_PROPERTY_AVATICA_USER);
                Preconditions.checkArgument(StringUtils.isNotEmpty(params.get(PHOENIX_THIN_PROPERTY_AVATICA_PASSWORD)), EMPTY_ERROR_MESSAGE_TEMPLATE, PHOENIX_THIN_PROPERTY_AVATICA_PASSWORD);
            } else {
                Preconditions.checkArgument(StringUtils.isNotEmpty(params.get(PHOENIX_THIN_PROPERTY_PRINCIPAL)), EMPTY_ERROR_MESSAGE_TEMPLATE, PHOENIX_THIN_PROPERTY_PRINCIPAL);
                Preconditions.checkArgument(StringUtils.isNotEmpty(params.get(PHOENIX_THIN_PROPERTY_KEYTAB)), EMPTY_ERROR_MESSAGE_TEMPLATE, PHOENIX_THIN_PROPERTY_KEYTAB);

            }
            ValidateUtils.validatePhoenixName(params.get(PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_DATA_TABLE_NAME), PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_DATA_TABLE_NAME);
            ValidateUtils.validatePhoenixName(params.get(PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_METADATA_TABLE_NAME), PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_METADATA_TABLE_NAME);
            ValidateUtils.validatePhoenixName(params.get(PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_NAME), PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_NAME);
            ValidateUtils.validatePhoenixName(params.get(PARAMS_PHOENIX_DB_QUERY_PROFILE_METADATA_TABLE_NAME), PARAMS_PHOENIX_DB_QUERY_PROFILE_METADATA_TABLE_NAME);
            ValidateUtils.validatePhoenixName(params.get(PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_NAME), PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_NAME);
            Preconditions.checkArgument(StringUtils.isNumeric(params.get(PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_START_WITH)), INCORRECT_NUMERIC_MESSAGE_TEMPLATE, PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_START_WITH, params.get(PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_START_WITH));
            Preconditions.checkArgument(StringUtils.isNumeric(params.get(PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_CACHE)), INCORRECT_NUMERIC_MESSAGE_TEMPLATE, PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_CACHE, params.get(PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_CACHE));
            Preconditions.checkArgument(StringUtils.isNumeric(params.get(PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_START_WITH)), INCORRECT_NUMERIC_MESSAGE_TEMPLATE, PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_START_WITH, params.get(PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_START_WITH));
            Preconditions.checkArgument(StringUtils.isNumeric(params.get(PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_CACHE)), INCORRECT_NUMERIC_MESSAGE_TEMPLATE, PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_CACHE, params.get(PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_CACHE));
            Preconditions.checkArgument(StringUtils.isNumeric(params.get(PARAMS_PHOENIX_DB_QUERY_KEY_COUNT)), INCORRECT_NUMERIC_MESSAGE_TEMPLATE, PARAMS_PHOENIX_DB_QUERY_KEY_COUNT, params.get(PARAMS_PHOENIX_DB_QUERY_KEY_COUNT));
        } else {
            Preconditions.checkArgument(StringUtils.equals(phoenixFlag, "false"), "Invalid properties '%s' value %s (expected 'true' or 'false').", PARAMS_PHOENIX_DB_INIT, phoenixFlag);
        }
    }

}
