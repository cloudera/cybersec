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

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.generator.FreemarkerImmediateGenerator;
import com.cloudera.cyber.profiler.dto.MeasurementDto;
import com.cloudera.cyber.profiler.dto.ProfileDto;
import com.cloudera.cyber.profiler.phoenix.PhoenixThinClient;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoredMessageWatermarkedStream;
import com.cloudera.cyber.scoring.ScoringJob;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.google.common.base.Preconditions;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;
import freemarker.template.TemplateException;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.windowing.assigners.SlidingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.assigners.TumblingEventTimeWindows;
import org.apache.flink.streaming.api.windowing.time.Time;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.ResultSetMetaData;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public abstract class ProfileJob {
    protected PhoenixThinClient client;
    protected static final String PARAM_PROFILE_CONFIG = "profile.config.file";
    protected static final String PARAM_LATENESS_TOLERANCE_MILLIS = "profile.lateness";
    protected static final String PARAMS_PHOENIX_DB_INIT = "phoenix.db.init";
    protected static final String PARAMS_PHOENIX_DB_QUERY_PARAM = "phoenix.db.query.param.";
    protected static final String PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_DATA_TABLE_NAME = "phoenix.db.query.param.measurement_data_table_name";
    protected static final String PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_METADATA_TABLE_NAME = "phoenix.db.query.param.measurement_metadata_table_name";
    protected static final String PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_NAME = "phoenix.db.query.param.measurement_sequence_name";
    protected static final String PARAMS_PHOENIX_DB_QUERY_PROFILE_METADATA_TABLE_NAME = "phoenix.db.query.param.profile_metadata_table_name";
    protected static final String PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_NAME = "phoenix.db.query.param.profile_sequence_name";
    protected static final String PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_START_WITH = "phoenix.db.query.param.measurement_sequence_start_with";
    protected static final String PARAMS_PHOENIX_DB_QUERY_MEASUREMENT_SEQUENCE_CACHE = "phoenix.db.query.param.measurement_sequence_cache";
    protected static final String PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_START_WITH = "phoenix.db.query.param.profile_sequence_start_with";
    protected static final String PARAMS_PHOENIX_DB_QUERY_PROFILE_SEQUENCE_CACHE = "phoenix.db.query.param.profile_sequence_cache";
    protected static final String PARAMS_PHOENIX_DB_QUERY_KEY_COUNT = "phoenix.db.query.param.field_key_count";
    protected static final String DRIVER_NAME = "org.apache.phoenix.jdbc.PhoenixDriver";
    protected static final ObjectMapper jsonObjectMapper =
            new ObjectMapper()
                    .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().
                            allowIfSubType(Map.class).
                            allowIfSubType(List.class).
                            allowIfSubType(java.util.concurrent.TimeUnit.class).
                            build())
                    .enable(SerializationFeature.INDENT_OUTPUT);
    private static final String UPSERT_INTO_MEASUREMENT_METADATA_TABLE = "UPSERT INTO ${measurement_metadata_table_name} (ID,FIELD_NAME,RESULT_EXTENSION_NAME,AGGREGATION_METHOD,CALCULATE_STATS,FORMAT,FIRST_SEEN_EXPIRATION_DURATION, FIRST_SEEN_EXPIRATION_DURATION_UNIT, PROFILE_ID) VALUES (NEXT VALUE FOR ${measurement_sequence_name},?,?,?,?,?,?,?,?) ON DUPLICATE KEY IGNORE";
    private static final Function<ResultSet, Integer> ID_MAPPER = rs -> {
        try {
            return rs.getInt("ID");
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    };
    private static final Function<ResultSet, Integer> FIRST_VALUE_MAPPER = rs -> {
        try {
            return rs.getInt(1);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    };
    private static final Function<ResultSet, ProfileDto> PROFILE_MAPPER = rs -> {
        try {
            return ProfileDto.builder().id(rs.getInt("ID"))
                    .profileGroupName(rs.getString("PROFILE_GROUP_NAME"))
                    .keyFieldNames(rs.getString("KEY_FIELD_NAMES"))
                    .periodDuration(rs.getLong("PERIOD_DURATION"))
                    .periodDurationUnit(rs.getString("PERIOD_DURATION_UNIT"))
                    .statsSlide(rs.getLong("STATS_SLIDE"))
                    .statsSlideUnit(rs.getString("STATS_SLIDE_UNIT"))
                    .build();
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
        return null;
    };
    protected final FreemarkerImmediateGenerator freemarkerGenerator = new FreemarkerImmediateGenerator();
    private static final String SELECT_MEASUREMENT_CONFIG_ID = "SELECT ID FROM ${measurement_metadata_table_name} WHERE FIELD_NAME <#if field_name??> = ?<#else> IS NULL</#if>\n" +
            "        AND RESULT_EXTENSION_NAME <#if result_extension_name??> = ?<#else> IS NULL</#if>\n" +
            "        AND AGGREGATION_METHOD <#if aggregation_method??> = ?<#else> IS NULL</#if>\n" +
            "        AND CALCULATE_STATS <#if calculate_stats??> = ?<#else> IS NULL</#if>\n" +
            "        AND FORMAT <#if format??> = ?<#else> IS NULL</#if>\n" +
            "        AND FIRST_SEEN_EXPIRATION_DURATION <#if first_seen_expiration_duration??> = ?<#else> IS NULL</#if>\n" +
            "        AND FIRST_SEEN_EXPIRATION_DURATION_UNIT <#if first_seen_expiration_duration_unit??> = ?<#else> IS NULL</#if>" +
            "        AND PROFILE_ID <#if profile_id??> = ?<#else> IS NULL</#if>";
    private static final String SELECT_PROFILE_BY_ID = "SELECT * FROM ${profile_metadata_table_name} WHERE PROFILE_GROUP_NAME = ?";
    private static final String UPDATE_PROFILE = "UPSERT INTO ${profile_metadata_table_name} (ID,PROFILE_GROUP_NAME,KEY_FIELD_NAMES,PERIOD_DURATION,PERIOD_DURATION_UNIT,STATS_SLIDE,STATS_SLIDE_UNIT) VALUES (?,?,?,?,?,?,?) ON DUPLICATE KEY IGNORE";
    private static final String SELECT_PROFILE_ID = "SELECT NEXT VALUE FOR ${profile_sequence_name}";
    private static final String SELECT_METADATA_FROM_MEASUREMENT_DATA_TABLE = "SELECT * FROM ${measurement_data_table_name} LIMIT 1";

    protected List<ProfileGroupConfig> parseConfigFile(String configJson) throws JsonProcessingException {
        List<ProfileGroupConfig> profileGroupConfigs = jsonObjectMapper.readValue(
                configJson,
                new TypeReference<ArrayList<ProfileGroupConfig>>() {
                });
        profileGroupConfigs.forEach(ProfileGroupConfig::verify);
        return profileGroupConfigs;
    }

    protected StreamExecutionEnvironment createPipeline(final ParameterTool params) throws IOException, SQLException, TemplateException {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        FlinkUtils.setupEnv(env, params);

        // set the JdbcCatalog as the current catalog of the session
        // read the profiler config
        List<ProfileGroupConfig> profileGroups = parseConfigFile(new String(Files.readAllBytes(Paths.get(params.getRequired(PARAM_PROFILE_CONFIG)))));

        List<ProfileDto> profileDtos = phoenixDataBaseInit(params, profileGroups);

        long allowedLatenessMillis = params.getLong(PARAM_LATENESS_TOLERANCE_MILLIS, Time.minutes(5).toMilliseconds());
        final DataStream<ScoredMessage> messages = ScoredMessageWatermarkedStream.of(createSource(env, params), allowedLatenessMillis);

        DataStream<ProfileMessage> profiledStreams = null;
        for (ProfileGroupConfig profileGroupConfig : profileGroups) {
            profiledStreams = profile(params, messages, profileGroupConfig, allowedLatenessMillis, profiledStreams);
        }

        Preconditions.checkNotNull(profiledStreams, "At least one profile must be specified");
        DataStream<ScoredMessage> scoredMessages = score(profiledStreams.map(new ProfileMessageToMessageMap()), env, params);
            writeProfileMeasurementsResults(params, profileDtos, profiledStreams);
        writeResults(params, scoredMessages);
        return env;
    }

    private List<ProfileDto> phoenixDataBaseInit(ParameterTool params, List<ProfileGroupConfig> profileGroups) throws SQLException, IOException, TemplateException {
        if (params.getBoolean(PARAMS_PHOENIX_DB_INIT)) {
            Properties properties = Utils.readProperties(params.getProperties(), PARAMS_PHOENIX_DB_QUERY_PARAM);
            client = new PhoenixThinClient(params);
            ImmutableMap<String, String> mapsParams = Maps.fromProperties(properties);
            client.executeSql(freemarkerGenerator.replaceByFile("sql/measurement_id_sequence.sql", mapsParams));
            client.executeSql(freemarkerGenerator.replaceByFile("sql/profile_id_sequence.sql", mapsParams));
            client.executeSql(freemarkerGenerator.replaceByFile("sql/profile_metadata_table.sql", mapsParams));
            client.executeSql(freemarkerGenerator.replaceByFile("sql/measurement_metadata_table.sql", mapsParams));
            client.executeSql(freemarkerGenerator.replaceByFile("sql/measurement_table.sql", mapsParams));
            ResultSetMetaData tableMetadata = client.getTableMetadata(freemarkerGenerator.replaceByTemplate(SELECT_METADATA_FROM_MEASUREMENT_DATA_TABLE, mapsParams));
            validate(params, tableMetadata);
            return persistProfileMeasurementConfigMeta(profileGroups, client, properties);
        }
        return Collections.emptyList();
    }

    private void phoenixParamValidate(ParameterTool param) {

    }

    private void validate(ParameterTool params, ResultSetMetaData tableMetadata) throws SQLException {
        int keyCount = params.getInt(PARAMS_PHOENIX_DB_QUERY_KEY_COUNT, 0);
        boolean valid = false;
        for (int i = 1; i <= tableMetadata.getColumnCount(); i++) {
            String columnName = tableMetadata.getColumnName(i);
            if (StringUtils.equals(columnName, "KEY_" + keyCount)) {
                valid = true;
            }
            if (StringUtils.equals(columnName, "KEY_" + (keyCount + 1))) {
                throw new IllegalStateException("The measurement table is incompatible. The number of key fields does not match " + keyCount + ".");
            }
        }
        if (!valid) {
            throw new IllegalStateException("The measurement table is incompatible. The number of key fields does not match " + keyCount + ".");
        }
    }

    protected List<ProfileDto> persistProfileMeasurementConfigMeta(List<ProfileGroupConfig> profileGroups, PhoenixThinClient client, Properties properties) throws SQLException, IOException, TemplateException {
        ImmutableMap<String, String> propertiesMap = Maps.fromProperties(properties);
        List<ProfileDto> profileDtos = profileGroups.stream().map(ProfileDto::of).collect(Collectors.toList());
        for (ProfileDto profileDto : profileDtos) {
            ProfileDto profileDtoDb = client.selectResultWithParams(freemarkerGenerator.replaceByTemplate(SELECT_PROFILE_BY_ID, propertiesMap), PROFILE_MAPPER, ps -> {
                try {
                    ps.setString(1, profileDto.getProfileGroupName());
                } catch (SQLException throwables) {
                    throwables.printStackTrace();
                }
            });
            if (profileDtoDb == null) {
                Integer profileId = client.selectResultWithParams(freemarkerGenerator.replaceByTemplate(SELECT_PROFILE_ID, propertiesMap), FIRST_VALUE_MAPPER, preparedStatement -> {
                });
                client.insertIntoTable(freemarkerGenerator.replaceByTemplate(UPDATE_PROFILE, propertiesMap), getUpdateProfileConsumer(profileDto, profileId));
                profileDto.setId(profileId);
            } else {
                if (!StringUtils.equals(profileDto.getKeyFieldNames(), profileDtoDb.getKeyFieldNames())) {
                    throw new IllegalStateException("Key fields cannot be changed for the profile.");
                }
                profileDto.setId(profileDtoDb.getId());
                client.insertIntoTable(freemarkerGenerator.replaceByTemplate(UPDATE_PROFILE, propertiesMap), getUpdateProfileConsumer(profileDto, profileDtoDb.getId()));
            }
            for (MeasurementDto measurement : profileDto.getMeasurementDtos()) {
                measurement.setProfileId(profileDto.getId());
                Map<String, String> params = getFreemarkerParams(measurement);
                params.putAll(propertiesMap);
                Integer id = client.selectResultWithParams(freemarkerGenerator.replaceByTemplate(SELECT_MEASUREMENT_CONFIG_ID, params), ID_MAPPER, getSelectMeasurementConfigConsumer(measurement));
                if (id == null) {
                    client.insertIntoTable(freemarkerGenerator.replaceByTemplate(UPSERT_INTO_MEASUREMENT_METADATA_TABLE, params), getInsertMeasurementConfigConsumer(measurement, profileDto.getId()));
                    id = client.selectResultWithParams(freemarkerGenerator.replaceByTemplate(SELECT_MEASUREMENT_CONFIG_ID, params), ID_MAPPER, getSelectMeasurementConfigConsumer(measurement));
                }
                measurement.setId(id);
            }
        }
        return profileDtos;
    }

    private Consumer<PreparedStatement> getInsertMeasurementConfigConsumer(MeasurementDto measurement, Integer profileId) {
        return ps -> {
            try {
                ps.setString(1, measurement.getFieldName());
                ps.setString(2, measurement.getResultExtensionName());
                ps.setString(3, Optional.ofNullable(measurement.getAggregationMethod()).map(Objects::toString).orElse(null));
                if (measurement.getCalculateStats() == null) {
                    ps.setNull(4, Types.BOOLEAN);
                } else {
                    ps.setBoolean(4, measurement.getCalculateStats());
                }
                ps.setString(5, measurement.getFormat());
                if (measurement.getFirstSeenExpirationDuration() == null) {
                    ps.setNull(6, Types.BIGINT);
                } else {
                    ps.setLong(6, measurement.getFirstSeenExpirationDuration());
                }
                ps.setString(7, measurement.getFirstSeenExpirationDurationUnit());
                ps.setInt(8, profileId);
            } catch (SQLException throwables) {
                throwables.printStackTrace();
            }
        };
    }

    private ThrowingConsumer<PreparedStatement> getUpdateProfileConsumer(ProfileDto profileDto, Integer profileId) {
        return ps -> {
            try {
                ps.setInt(1, profileId);
                ps.setString(2, profileDto.getProfileGroupName());
                ps.setString(3, profileDto.getKeyFieldNames());
                if (profileDto.getPeriodDuration() == null) {
                    ps.setNull(4, Types.BIGINT);
                } else {
                    ps.setLong(4, profileDto.getPeriodDuration());
                }
                ps.setString(5, profileDto.getPeriodDurationUnit());
                if (profileDto.getStatsSlide() == null) {
                    ps.setNull(6, Types.BIGINT);
                } else {
                    ps.setLong(6, profileDto.getStatsSlide());
                }
                ps.setString(7, profileDto.getStatsSlideUnit());
            } catch (SQLException throwables) {
                log.error(throwables.getMessage());
                throw throwables;
            }
        };
    }


    private ThrowingConsumer<PreparedStatement> getSelectMeasurementConfigConsumer(MeasurementDto measurement) {
        return ps -> {
            try {
                int count = 1;
                if (measurement.getFieldName() != null) {
                    ps.setString(count++, measurement.getFieldName());
                }
                if (measurement.getResultExtensionName() != null) {
                    ps.setString(count++, measurement.getResultExtensionName());
                }
                if (measurement.getAggregationMethod() != null) {
                    ps.setString(count++, measurement.getAggregationMethod());
                }
                if (measurement.getCalculateStats() != null) {
                    ps.setBoolean(count++, measurement.getCalculateStats());
                }
                if (measurement.getFormat() != null) {
                    ps.setString(count++, measurement.getFormat());
                }
                if (measurement.getFirstSeenExpirationDuration() != null) {
                    ps.setLong(count++, measurement.getFirstSeenExpirationDuration());
                }
                if (measurement.getFirstSeenExpirationDurationUnit() != null) {
                    ps.setString(count++, measurement.getFirstSeenExpirationDurationUnit());
                }
                if (measurement.getProfileId() != null) {
                    ps.setLong(count, measurement.getProfileId());
                }
            } catch (SQLException throwables) {
                log.error(throwables.getMessage());
                throw throwables;
            }
        };
    }

    private Map<String, String> getFreemarkerParams(MeasurementDto measurementDto) {
        Map<String, String> config = new HashMap<>();
        config.put("field_name", measurementDto.getFieldName());
        config.put("result_extension_name", measurementDto.getResultExtensionName());
        config.put("aggregation_method", Optional.ofNullable(measurementDto.getAggregationMethod()).map(Objects::toString).orElse(null));
        config.put("calculate_stats", Optional.ofNullable(measurementDto.getCalculateStats()).map(Objects::toString).orElse(null));
        config.put("format", measurementDto.getFormat());
        config.put("first_seen_expiration_duration", Optional.ofNullable(measurementDto.getFirstSeenExpirationDuration()).map(Objects::toString).orElse(null));
        config.put("first_seen_expiration_duration_unit", measurementDto.getFirstSeenExpirationDurationUnit());
        config.put("profile_id", Optional.ofNullable(measurementDto.getProfileId()).map(Objects::toString).orElse(null));
        return config;
    }

    /**
     * Aggregate events into profile messages for this ProfileGroup.
     *
     * @param params   Configuration parameters
     * @param messages Data stream of messages to profile.
     */
    protected DataStream<ProfileMessage> profile(final ParameterTool params, DataStream<ScoredMessage> messages, ProfileGroupConfig profileGroupConfig, long allowedLatenessMillis, DataStream<ProfileMessage> profileMessageStreams) {

        Time profilePeriodDuration = Time.of(profileGroupConfig.getPeriodDuration(), TimeUnit.valueOf(profileGroupConfig.getPeriodDurationUnit()));
        DataStream<ProfileMessage> profileMessages = messages.filter(new ProfileMessageFilter(profileGroupConfig)).
                map(new ScoredMessageToProfileMessageMap(profileGroupConfig)).
                keyBy(new MessageKeySelector(profileGroupConfig.getKeyFieldNames())).window(TumblingEventTimeWindows.of(profilePeriodDuration)).
                aggregate(new FieldValueProfileAggregateFunction(profileGroupConfig));
        if (profileGroupConfig.hasFirstSeen()) {
            profileMessages = updateFirstSeen(params, profileMessages, profileGroupConfig);
        }

        if (profileGroupConfig.hasStats()) {
            MessageKeySelector profileKeySelector = new MessageKeySelector(Collections.singletonList(ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION));
            Time statsSlide = Time.of(profileGroupConfig.getStatsSlide(), TimeUnit.valueOf(profileGroupConfig.getStatsSlideUnit()));

            DataStream<ProfileMessage> statsStream = ProfileMessage.watermarkedStreamOf(profileMessages, allowedLatenessMillis).
                    keyBy(profileKeySelector).
                    window(SlidingEventTimeWindows.of(profilePeriodDuration, statsSlide)).
                    aggregate(new StatsProfileAggregateFunction(profileGroupConfig));
            StatsProfileKeySelector statsKeySelector = new StatsProfileKeySelector();
            profileMessages = profileMessages.join(statsStream).where(profileKeySelector).equalTo(statsKeySelector).window(TumblingEventTimeWindows.of(profilePeriodDuration)).apply(new ProfileStatsJoin());
            profileMessageStreams = unionProfileMessages(profileMessageStreams, statsStream);
        }

        return unionProfileMessages(profileMessageStreams, profileMessages);
    }

    private DataStream<ProfileMessage> unionProfileMessages(DataStream<ProfileMessage> profileMessageUnion, DataStream<ProfileMessage> newStream) {
        if (profileMessageUnion == null) {
            return newStream;
        } else {
            return profileMessageUnion.union(newStream);
        }
    }

    protected abstract void writeProfileMeasurementsResults(ParameterTool params, List<ProfileDto> profileDtos, DataStream<ProfileMessage> results) throws IOException, TemplateException;

    protected abstract DataStream<ScoredMessage> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeResults(ParameterTool params, DataStream<ScoredMessage> results);

    protected abstract DataStream<ProfileMessage> updateFirstSeen(ParameterTool params, DataStream<ProfileMessage> results, ProfileGroupConfig profileGroupConfig);

    protected abstract DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeScoredRuleCommandResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results);

    private DataStream<ScoredMessage> score(DataStream<Message> in, StreamExecutionEnvironment env, ParameterTool params) {
        DataStream<ScoringRuleCommand> rulesSource = createRulesSource(env, params);
        SingleOutputStreamOperator<ScoredMessage> results = ScoringJob.enrich(in, rulesSource);
        writeScoredRuleCommandResult(params, results.getSideOutput(ScoringJob.COMMAND_RESULT_OUTPUT_TAG));
        return results;
    }

    public interface ThrowingConsumer<T> extends Consumer<T> {
        @Override
        default void accept(final T elem) {
            try {
                acceptThrows(elem);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }

        void acceptThrows(T elem) throws Exception;
    }
}
