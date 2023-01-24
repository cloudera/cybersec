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
import com.cloudera.cyber.profiler.dto.MeasurementDto.MeasurementDtoBuilder;
import com.cloudera.cyber.profiler.dto.ProfileDto;
import com.cloudera.cyber.profiler.dto.ProfileDto.ProfileDtoBuilder;
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
import java.util.function.BiFunction;
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
    protected static final ObjectMapper jsonObjectMapper =
        new ObjectMapper()
            .activateDefaultTyping(BasicPolymorphicTypeValidator.builder().
                allowIfSubType(Map.class).
                allowIfSubType(List.class).
                allowIfSubType(java.util.concurrent.TimeUnit.class).
                build())
            .enable(SerializationFeature.INDENT_OUTPUT);

    private static final Function<ResultSet, Integer> ID_MAPPER = rs -> {
        try {
            return rs.getInt("ID");
        } catch (SQLException e) {
            log.error("Error while mapping an id", e);
            return null;
        }
    };
    private static final Function<ResultSet, Integer> FIRST_VALUE_MAPPER = rs -> {
        try {
            return rs.getInt(1);
        } catch (SQLException e) {
            log.error("Error while mapping a first value", e);
            return null;
        }
    };
    private static final Function<ResultSet, ProfileDto> PROFILE_META_MAPPER = rs -> {
        try {
            ProfileDtoBuilder profileDtoBuilder = ProfileDto.builder().id(rs.getInt("ID"))
                .profileGroupName(rs.getString("PROFILE_GROUP_NAME"))
                .keyFieldNames(rs.getString("KEY_FIELD_NAMES"));
            long periodDuration = rs.getLong("PERIOD_DURATION");
            if (!rs.wasNull()) {
                profileDtoBuilder.periodDuration(periodDuration)
                    .periodDurationUnit(rs.getString("PERIOD_DURATION_UNIT"));
            }
            long statsSlide = rs.getLong("STATS_SLIDE");
            if (!rs.wasNull()) {
                profileDtoBuilder.statsSlide(statsSlide)
                    .statsSlideUnit(rs.getString("STATS_SLIDE_UNIT"));
            }
            return profileDtoBuilder.build();
        } catch (SQLException e) {
            log.error("Error while mapping a profile", e);
            return ProfileDto.builder().build();
        }
    };

    private static final Function<ResultSet, MeasurementDto> MEASUREMENT_META_MAPPER = rs -> {
        try {
            MeasurementDtoBuilder format = MeasurementDto.builder()
                .id(rs.getInt("ID"))
                .resultExtensionName(rs.getString("RESULT_EXTENSION_NAME"))
                .calculateStats(rs.getBoolean("CALCULATE_STATS"))
                .aggregationMethod(rs.getString("AGGREGATION_METHOD"))
                .fieldName(rs.getString("FIELD_NAME"))
                .format(rs.getString("FORMAT"));
            long firstSeenExpirationDuration = rs.getLong("FIRST_SEEN_EXPIRATION_DURATION");
            if (!rs.wasNull()) {
                format.firstSeenExpirationDuration(firstSeenExpirationDuration)
                    .firstSeenExpirationDurationUnit(rs.getString("FIRST_SEEN_EXPIRATION_DURATION_UNIT"));
            }
            format.profileId(rs.getInt("PROFILE_ID"));
            return format.build();
        } catch (SQLException e) {
            log.error("Error while mapping a measurement", e);
            return MeasurementDto.builder().build();
        }
    };
    protected final FreemarkerImmediateGenerator freemarkerGenerator = new FreemarkerImmediateGenerator();
    private static final String SELECT_PROFILE_BY_NAME = "SELECT * FROM ${profile_metadata_table_name} WHERE PROFILE_GROUP_NAME = ?";
    private static final String SELECT_MEASUREMENT_BY_NAME = "SELECT * FROM ${measurement_metadata_table_name} WHERE RESULT_EXTENSION_NAME = ? AND PROFILE_ID = ?";
    private static final String UPSERT_PROFILE_META = "UPSERT INTO ${profile_metadata_table_name} (ID,PROFILE_GROUP_NAME,KEY_FIELD_NAMES,PERIOD_DURATION,PERIOD_DURATION_UNIT,STATS_SLIDE,STATS_SLIDE_UNIT) VALUES (?,?,?,?,?,?,?)";
    private static final String UPSERT_MEASUREMENT_META = "UPSERT INTO ${measurement_metadata_table_name} (ID,FIELD_NAME,RESULT_EXTENSION_NAME,AGGREGATION_METHOD,CALCULATE_STATS,FORMAT,FIRST_SEEN_EXPIRATION_DURATION, FIRST_SEEN_EXPIRATION_DURATION_UNIT, PROFILE_ID) VALUES (?,?,?,?,?,?,?,?,?)";
    private static final String SELECT_PROFILE_ID = "SELECT NEXT VALUE FOR ${profile_sequence_name}";
    private static final String SELECT_MEASUREMENT_ID = "SELECT NEXT VALUE FOR ${measurement_sequence_name}";
    private static final String SELECT_METADATA_FROM_MEASUREMENT_DATA_TABLE = "SELECT * FROM ${measurement_data_table_name} LIMIT 1";

    private static final Function<String, Consumer<PreparedStatement>> PROFILE_PS_CONSUMER_FUNCTION = profileGroupName -> ps -> {
        try {
            ps.setString(1, profileGroupName);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    };

    private static final BiFunction<String, Integer, Consumer<PreparedStatement>> MEASUREMENT_PS_CONSUMER_FUNCTION = (resultExtensionName, profileId) -> ps -> {
        try {
            ps.setString(1, resultExtensionName);
            ps.setInt(2, profileId);
        } catch (SQLException throwables) {
            throwables.printStackTrace();
        }
    };

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
        List<ProfileDto> resultProfiles = new ArrayList<>();
        for (ProfileDto profile : profileGroups.stream().map(ProfileDto::of).collect(Collectors.toList())) {
            resultProfiles.add(processProfile(client, propertiesMap, profile));
        }
        return resultProfiles;
    }

    private MeasurementDto processMeasurement(PhoenixThinClient client, ImmutableMap<String, String> propertiesMap, Integer profileId, MeasurementDto measurement) throws SQLException, IOException, TemplateException {
        MeasurementDto measurementEntity = client.selectResult(freemarkerGenerator.replaceByTemplate(SELECT_MEASUREMENT_BY_NAME, propertiesMap), MEASUREMENT_META_MAPPER,
            MEASUREMENT_PS_CONSUMER_FUNCTION.apply(measurement.getResultExtensionName(), profileId));
        if (measurementEntity == null) {
            return createMeasurement(client, propertiesMap, profileId, measurement);
        } else if (measurementEntity.getId() == null) {
            throw new RuntimeException(String.format("Error when trying to map measurement from phoenix database with name %s and profile id %d", measurement.getResultExtensionName(), measurement.getProfileId()));
        } else {
            return updateMeasurement(client, propertiesMap, profileId, measurementEntity, measurement);
        }
    }

    private MeasurementDto createMeasurement(PhoenixThinClient client, Map<String, String> params, Integer profileId, MeasurementDto measurement) throws SQLException, IOException, TemplateException {
        Integer measurementId = client.selectResult(freemarkerGenerator.replaceByTemplate(SELECT_MEASUREMENT_ID, params), FIRST_VALUE_MAPPER);
        client.insertIntoTable(freemarkerGenerator.replaceByTemplate(UPSERT_MEASUREMENT_META, params), getUpdateMeasurementMetaConsumer(measurement, measurementId, profileId));
        return measurement.toBuilder().id(measurementId).profileId(profileId).build();
    }

    private MeasurementDto updateMeasurement(PhoenixThinClient client, Map<String, String> params, Integer profileId, MeasurementDto measurementDtoDb, MeasurementDto measurement) throws SQLException, IOException, TemplateException {
        if (!ProfileUtils.compareMeasurementDto(measurementDtoDb, measurement)) {
            log.error("Measurement from configuration {} measurement from database {}", measurement, measurementDtoDb);
            throw new IllegalStateException("Key fields cannot be changed for the measurement='" + measurement + "' measurementDtoDb='" + measurementDtoDb + "'");
        }
        Integer measurementId = measurementDtoDb.getId();
        client.insertIntoTable(freemarkerGenerator.replaceByTemplate(UPSERT_MEASUREMENT_META, params), getUpdateMeasurementMetaConsumer(measurement, measurementId, profileId));
        return measurement.toBuilder().id(measurementId).profileId(profileId).build();
    }

    private ProfileDto processProfile(PhoenixThinClient client, ImmutableMap<String, String> propertiesMap, ProfileDto profile) throws SQLException, IOException, TemplateException {
        String profileGroupName = profile.getProfileGroupName();
        ProfileDto profileDtoDb = client.selectResult(freemarkerGenerator.replaceByTemplate(SELECT_PROFILE_BY_NAME, propertiesMap), PROFILE_META_MAPPER, PROFILE_PS_CONSUMER_FUNCTION.apply(profileGroupName));
        if (profileDtoDb == null) {
            return createProfile(client, propertiesMap, profile);
        } else if (profileDtoDb.getId() == null) {
            throw new SQLException(String.format("Error when trying to map profile from phoenix database with name %s", profileGroupName));

        }else {
            return updateProfile(client, propertiesMap, profile, profileDtoDb);
        }
    }

    private ProfileDto updateProfile(PhoenixThinClient client, ImmutableMap<String, String> propertiesMap, ProfileDto profile, ProfileDto profileDtoDb) throws SQLException, IOException, TemplateException {
        if (!ProfileUtils.compareProfileDto(profileDtoDb, profile)) {
            log.error("Profile from configuration {} profile from database {}", profile, profileDtoDb);
            throw new IllegalStateException("Key fields cannot be changed for the profile='" + profile + "' profileDtoDb='" + profileDtoDb + "'");
        }
        Integer profileId = profileDtoDb.getId();
        client.insertIntoTable(freemarkerGenerator.replaceByTemplate(UPSERT_PROFILE_META, propertiesMap), getUpdateProfileConsumer(profile, profileId));
        ArrayList<MeasurementDto> measurementDtos = new ArrayList<>();
        for (MeasurementDto measurementDto : profile.getMeasurementDtos()) {
            measurementDtos.add(processMeasurement(client, propertiesMap, profileId, measurementDto));
        }
        return profile.toBuilder().id(profileId).measurementDtos(measurementDtos).build();
    }

    private ProfileDto createProfile(PhoenixThinClient client, ImmutableMap<String, String> propertiesMap, ProfileDto profile) throws SQLException, IOException, TemplateException {
        Integer profileId = client.selectResult(freemarkerGenerator.replaceByTemplate(SELECT_PROFILE_ID, propertiesMap), FIRST_VALUE_MAPPER);
        client.insertIntoTable(freemarkerGenerator.replaceByTemplate(UPSERT_PROFILE_META, propertiesMap), getUpdateProfileConsumer(profile, profileId));
        ArrayList<MeasurementDto> measurementDtos = new ArrayList<>();
        for (MeasurementDto measurementDto : profile.getMeasurementDtos()) {
            measurementDtos.add(createMeasurement(client, propertiesMap, profileId, measurementDto));
        }
        return profile.toBuilder().id(profileId).measurementDtos(measurementDtos).build();
    }

    private Consumer<PreparedStatement> getUpdateMeasurementMetaConsumer(MeasurementDto measurement, Integer measurementId, Integer profileId) {
        return ps -> {
            try {
                ps.setInt(1, measurementId);
                ps.setString(2, measurement.getFieldName());
                ps.setString(3, measurement.getResultExtensionName());
                ps.setString(4, Optional.ofNullable(measurement.getAggregationMethod()).map(Objects::toString).orElse(null));
                if (measurement.getCalculateStats() == null) {
                    ps.setNull(5, Types.BOOLEAN);
                } else {
                    ps.setBoolean(5, measurement.getCalculateStats());
                }
                ps.setString(6, measurement.getFormat());
                if (measurement.getFirstSeenExpirationDuration() == null) {
                    ps.setNull(7, Types.BIGINT);
                } else {
                    ps.setLong(7, measurement.getFirstSeenExpirationDuration());
                }
                ps.setString(8, measurement.getFirstSeenExpirationDurationUnit());
                ps.setInt(9, profileId);
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
