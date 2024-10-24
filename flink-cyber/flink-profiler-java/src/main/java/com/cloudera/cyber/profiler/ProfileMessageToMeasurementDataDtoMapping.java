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

import static com.cloudera.cyber.profiler.ProfileAggregateFunction.PROFILE_GROUP_NAME_EXTENSION;
import static com.cloudera.cyber.profiler.StatsProfileAggregateFunction.STATS_PROFILE_GROUP_SUFFIX;
import static com.cloudera.cyber.profiler.accumulator.StatsProfileGroupAcc.STATS_EXTENSION_SUFFIXES;

import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import com.cloudera.cyber.profiler.dto.MeasurementDataDto;
import com.cloudera.cyber.profiler.dto.MeasurementDto;
import com.cloudera.cyber.profiler.dto.ProfileDto;
import java.sql.Timestamp;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

@RequiredArgsConstructor
public class ProfileMessageToMeasurementDataDtoMapping extends RichFlatMapFunction<ProfileMessage, MeasurementDataDto> {
    private final ArrayList<ProfileDto> profileDtos;
    private HashMap<String, String[]> profileNameToKeyFieldNames;
    private HashMap<String, ProfileDto> profileNameToProfileDto;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.profileNameToKeyFieldNames =
              this.profileDtos.stream().collect(Collectors.toMap(ProfileDto::getProfileGroupName,
                    profileDto -> profileDto.getKeyFieldNames().split(","),
                    (first, second) -> first,
                    HashMap::new));
        this.profileNameToProfileDto =
              this.profileDtos.stream().collect(Collectors.toMap(ProfileDto::getProfileGroupName,
                    Function.identity(),
                    (first, second) -> first,
                    HashMap::new));
    }

    @Override
    public void flatMap(ProfileMessage profileMessage, Collector<MeasurementDataDto> collector) {
        if (profileMessage.getExtensions().get(PROFILE_GROUP_NAME_EXTENSION).endsWith(STATS_PROFILE_GROUP_SUFFIX)) {
            flatMapStatsProfile(profileMessage, collector);
        } else {
            flatMapProfile(profileMessage, collector);
        }
    }


    private void flatMapStatsProfile(ProfileMessage profileMessage, Collector<MeasurementDataDto> collector) {
        profileMessage.getExtensions().forEach((key, value) -> {
            if (STATS_EXTENSION_SUFFIXES.stream().anyMatch(key::endsWith)) {
                String[] parts = key.split("\\.");
                MeasurementDataDto measurementDto =
                      MeasurementDataDto.builder()
                                        .measurementId(
                                              getMeasurementIdByName(parts[0]).orElse(-1))
                                        .measurementName(parts[0])
                                        .measurementType(parts[1])
                                        .measurementTime(
                                              Timestamp.from(Instant.ofEpochMilli(
                                                    Long.parseLong(
                                                          profileMessage.getExtensions()
                                                                        .get(ProfileGroupAcc.START_PERIOD_EXTENSION)))))
                                        .measurementValue(Double.parseDouble(
                                              Optional.ofNullable(value).orElse("0")))
                                        .build();
                collector.collect(measurementDto);
            }
        });
    }

    private Optional<Integer> getMeasurementIdByName(String measurementName) {
        return profileDtos.stream().flatMap(profile -> profile.getMeasurementDtos().stream())
                          .filter(measurementDto -> measurementDto.getResultExtensionName().equals(measurementName))
                          .findFirst().map(MeasurementDto::getId);
    }

    private void flatMapProfile(ProfileMessage profileMessage, Collector<MeasurementDataDto> collector) {
        String profileGroupName = profileMessage.getExtensions().get(PROFILE_GROUP_NAME_EXTENSION);
        ProfileDto profileDto = profileNameToProfileDto.get(profileGroupName);
        if (profileDto != null) {
            final ArrayList<String> keyFieldNamesValues =
                  Arrays.stream(profileNameToKeyFieldNames.get(profileGroupName))
                        .map(keyFieldName -> profileMessage.getExtensions().get(keyFieldName))
                        .collect(Collectors.toCollection(ArrayList::new));
            for (MeasurementDto measurementDto : profileDto.getMeasurementDtos()) {
                String measurementName = measurementDto.getResultExtensionName();
                MeasurementDataDto measurementDataDto =
                      MeasurementDataDto.builder()
                                        .measurementId(measurementDto.getId())
                                        .keys(keyFieldNamesValues)
                                        .measurementName(measurementName)
                                        .profileId(profileDto.getId())
                                        .measurementTime(
                                              Timestamp.from(Instant.ofEpochMilli(
                                                    Long.parseLong(
                                                          profileMessage.getExtensions()
                                                                        .get(ProfileGroupAcc.START_PERIOD_EXTENSION)))))
                                        .measurementValue(Double.parseDouble(
                                              Optional.ofNullable(
                                                            profileMessage.getExtensions())
                                                      .map(map -> map.get(
                                                            measurementName))
                                                      .orElse("0")))
                                        .build();
                collector.collect(measurementDataDto);
            }
        }
    }
}
