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

package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.profiler.ProfileGroupConfig;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import com.cloudera.cyber.profiler.ProfileMessage;
import com.google.common.collect.Lists;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.flink.api.common.accumulators.Accumulator;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class StatsProfileGroupAcc extends ProfileGroupAcc {
    public static final String MIN_RESULT_SUFFIX = ".min";
    public static final String MAX_RESULT_SUFFIX = ".max";
    public static final String MEAN_RESULT_SUFFIX = ".mean";
    public static final String STDDEV_RESULT_SUFFIX = ".stddev";
    public static final List<String> STATS_EXTENSION_SUFFIXES = Lists.newArrayList(MIN_RESULT_SUFFIX, MAX_RESULT_SUFFIX, MEAN_RESULT_SUFFIX, STDDEV_RESULT_SUFFIX);
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.00");

    public StatsProfileGroupAcc(ProfileGroupConfig profileGroupConfig) {
        super(profileGroupConfig.getMeasurements().stream().filter(ProfileMeasurementConfig::hasStats).
                map(config -> new StatsAcc()).collect(Collectors.toList()));
    }

    @Override
    protected void updateAccumulators(ProfileMessage message, ProfileGroupConfig profileGroupConfig) {
        Iterator<ProfileMeasurementConfig> measurementIter = profileGroupConfig.getMeasurements().stream().
                filter(ProfileMeasurementConfig::hasStats).iterator();
        Iterator<Accumulator<?, ? extends Serializable>> accumulatorIter= accumulators.iterator();
        while(measurementIter.hasNext() && accumulatorIter.hasNext()) {
            ProfileMeasurementConfig measurementConfig = measurementIter.next();
            StatsAcc accumulator = (StatsAcc)accumulatorIter.next();
            Double fieldValue = getFieldValueAsDouble(message, measurementConfig.getResultExtensionName());
            if (fieldValue != null) {
                accumulator.getLocalValue().get(0).addValue(fieldValue);
            }
        }
    }

    @Override
    protected void addExtensions(ProfileGroupConfig profileGroupConfig, Map<String, String> extensions, Map<String, DecimalFormat> ignored) {
        Iterator<ProfileMeasurementConfig> measurementIter = profileGroupConfig.getMeasurements().stream().
                filter(ProfileMeasurementConfig::hasStats).iterator();
        Iterator<Accumulator<?, ? extends Serializable>> accumulatorIter= accumulators.iterator();
        while(measurementIter.hasNext() && accumulatorIter.hasNext()) {
            ProfileMeasurementConfig measurementConfig = measurementIter.next();
            StatsAcc accumulator = (StatsAcc)accumulatorIter.next();

            StatisticalSummaryValues stats = AggregateSummaryStatistics.aggregate(accumulator.getLocalValue());
            String resultExtensionName = measurementConfig.getResultExtensionName();
            extensions.put(resultExtensionName.concat(MIN_RESULT_SUFFIX), DEFAULT_FORMAT.format(stats.getMin()));
            extensions.put(resultExtensionName.concat(MAX_RESULT_SUFFIX), DEFAULT_FORMAT.format(stats.getMax()));
            extensions.put(resultExtensionName.concat(MEAN_RESULT_SUFFIX), DEFAULT_FORMAT.format(stats.getMean()));
            extensions.put(resultExtensionName.concat(STDDEV_RESULT_SUFFIX), DEFAULT_FORMAT.format(stats.getStandardDeviation()));
        }
    }
}
