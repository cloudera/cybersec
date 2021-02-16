package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.profiler.ProfileAggregateFunction;
import com.cloudera.cyber.profiler.ProfileAggregationMethod;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;

import java.text.DecimalFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class ProfileAccumulatorFactory {
    private static final Map<ProfileAggregationMethod, Function<ProfileMeasurementConfig, ProfileAccumulator>> factoryMap =
            new HashMap<ProfileAggregationMethod, Function<ProfileMeasurementConfig, ProfileAccumulator>>()
    {{
        put(ProfileAggregationMethod.COUNT, ProfileAccumulatorFactory::createCount);
        put(ProfileAggregationMethod.SUM, ProfileAccumulatorFactory::createSum);
        put(ProfileAggregationMethod.MIN, ProfileAccumulatorFactory::createMin);
        put(ProfileAggregationMethod.MAX, ProfileAccumulatorFactory::createMax);
        put(ProfileAggregationMethod.COUNT_DISTINCT, ProfileAccumulatorFactory::createCountDistinct);
    }};

    private static ProfileAccumulator createCount(ProfileMeasurementConfig measurementConfig) {
        return new CountProfileAccumulator(measurementConfig.getResultExtensionName(), getDecimalFormat(measurementConfig));
    }

    private static ProfileAccumulator createSum(ProfileMeasurementConfig measurementConfig) {
        return new SumProfileAccumulator(measurementConfig.getResultExtensionName(), measurementConfig.getFieldName(), getDecimalFormat(measurementConfig));
    }

    private static ProfileAccumulator createMin(ProfileMeasurementConfig measurementConfig) {
        return new MinimumProfileAccumulator(measurementConfig.getResultExtensionName(), measurementConfig.getFieldName(), getDecimalFormat(measurementConfig));
    }

    private static ProfileAccumulator createMax(ProfileMeasurementConfig measurementConfig) {
        return new MaximumProfileAccumulator(measurementConfig.getResultExtensionName(), measurementConfig.getFieldName(), getDecimalFormat(measurementConfig));
    }

    private static ProfileAccumulator createCountDistinct(ProfileMeasurementConfig measurementConfig) {
        return new CountDistinctProfileAccumulator(measurementConfig.getResultExtensionName(), measurementConfig.getFieldName(), getDecimalFormat(measurementConfig));
    }

    public static ProfileAccumulator createStats(ProfileMeasurementConfig measurementConfig) {
        return new StatsProfileAccumulator(measurementConfig.getResultExtensionName(), measurementConfig.getResultExtensionName(), getDecimalFormat(measurementConfig));
    }

    private static DecimalFormat getDecimalFormat(ProfileMeasurementConfig profileMeasurementConfig) {
        String decimalFormatString = profileMeasurementConfig.getFormat();
        if (decimalFormatString != null) {
            return new DecimalFormat(decimalFormatString);
        } else {
            return null;
        }
    }

    public static ProfileAccumulator create(ProfileMeasurementConfig measurementConfig) {
       return factoryMap.get(measurementConfig.getAggregationMethod()).apply(measurementConfig);
    }

}
