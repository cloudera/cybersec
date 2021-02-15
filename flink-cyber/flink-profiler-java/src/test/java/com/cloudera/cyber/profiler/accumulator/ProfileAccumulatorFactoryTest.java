package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.profiler.ProfileAggregationMethod;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import org.junit.Assert;
import org.junit.Test;

public class ProfileAccumulatorFactoryTest {

    @Test
    public void createCountAccumulator() {
        String countResultName = "count";
        ProfileMeasurementConfig countConfig = ProfileMeasurementConfig.builder().
                aggregationMethod(ProfileAggregationMethod.COUNT).
                resultExtensionName(countResultName).build();
        ProfileAccumulator acc = ProfileAccumulatorFactory.create(countConfig);
        Assert.assertTrue(acc instanceof CountProfileAccumulator);
        Assert.assertEquals(countResultName, acc.getResultExtensionName());
    }

    @Test
    public void createCountDistinctAccumulator() {
        verifyAccumulatorCreated(ProfileAggregationMethod.COUNT_DISTINCT, CountDistinctProfileAccumulator.class);
    }

    @Test
    public void createSumAccumulator() {
        verifyAccumulatorCreated(ProfileAggregationMethod.SUM, SumProfileAccumulator.class);
    }

    @Test
    public void createMinAccumulator() {
        verifyAccumulatorCreated(ProfileAggregationMethod.MIN, MinimumProfileAccumulator.class);
    }

    @Test
    public void createMaxAccumulator() {
        verifyAccumulatorCreated(ProfileAggregationMethod.MAX, MaximumProfileAccumulator.class);
    }


    private void verifyAccumulatorCreated(ProfileAggregationMethod aggregationMethod, Class<? extends ProfileAccumulator> expectedClass) {
        String resultName = "result";
        String fieldName = "field_name";
        ProfileMeasurementConfig config = ProfileMeasurementConfig.builder().
                aggregationMethod(aggregationMethod).
                resultExtensionName(resultName).
                fieldName(fieldName).
                build();
        ProfileAccumulator acc = ProfileAccumulatorFactory.create(config);
        Assert.assertEquals(expectedClass.getName(), acc.getClass().getName());
        Assert.assertEquals(resultName, acc.getResultExtensionName());
        if (acc instanceof FieldProfileAccumulator) {
            Assert.assertEquals(fieldName, ((FieldProfileAccumulator) acc).getFieldName());
        } else if (acc instanceof CountDistinctProfileAccumulator) {
            Assert.assertEquals(fieldName, ((CountDistinctProfileAccumulator)acc).getFieldName());
        }
    }
}
