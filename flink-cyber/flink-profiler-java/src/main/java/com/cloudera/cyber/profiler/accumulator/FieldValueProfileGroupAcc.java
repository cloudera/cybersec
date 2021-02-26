package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.profiler.ProfileAggregationMethod;
import com.cloudera.cyber.profiler.ProfileGroupConfig;
import com.cloudera.cyber.profiler.ProfileMeasurementConfig;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.accumulators.*;

import java.io.Serializable;
import java.text.DecimalFormat;
import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import static com.cloudera.cyber.profiler.ProfileAggregationMethod.*;

@Slf4j
public class FieldValueProfileGroupAcc extends ProfileGroupAcc {

    private static final Map<ProfileAggregationMethod, Supplier<Accumulator<?, ? extends Serializable>>> accFactory =
            new HashMap<ProfileAggregationMethod, Supplier<Accumulator<?, ? extends Serializable>>>() {{
                put(SUM, DoubleCounter::new);
                put(COUNT, DoubleCounter::new);
                put(MIN, DoubleMinimum::new);
                put(MAX, DoubleMaximum::new);
                put(COUNT_DISTINCT, CountDistinctAcc::new);
                put(FIRST_SEEN, LongMinimum::new);
            }};

    @FunctionalInterface
    interface ProfileUpdateFunction {
        void update(ProfileMeasurementConfig config, Message message, Accumulator<?, ? extends Serializable> acc);
    }

    private static final Map<ProfileAggregationMethod, ProfileUpdateFunction> accUpdate =
            new HashMap<ProfileAggregationMethod, ProfileUpdateFunction>() {{
                put(SUM, FieldValueProfileGroupAcc::updateDoubleAccumulator);
                put(COUNT, FieldValueProfileGroupAcc::updateCounterAccumulator);
                put(MIN, FieldValueProfileGroupAcc::updateDoubleAccumulator);
                put(MAX, FieldValueProfileGroupAcc::updateDoubleAccumulator);
                put(COUNT_DISTINCT, FieldValueProfileGroupAcc::updateStringAccumulator);
                put(FIRST_SEEN, FieldValueProfileGroupAcc::updateTimestampAccumulator);
            }};


    @FunctionalInterface
    interface ProfileExtensionFunction {
        void update(ProfileMeasurementConfig config , Accumulator<?, ? extends Serializable> acc, Map<String, String> extensions, DecimalFormat format);
    }

    private static final Map<ProfileAggregationMethod, ProfileExtensionFunction> extensionUpdate =
            new HashMap<ProfileAggregationMethod, ProfileExtensionFunction>() {{
                put(SUM, FieldValueProfileGroupAcc::getDoubleAccumulatorExtensions);
                put(COUNT, FieldValueProfileGroupAcc::getDoubleAccumulatorExtensions);
                put(MIN, FieldValueProfileGroupAcc::getDoubleAccumulatorExtensions);
                put(MAX, FieldValueProfileGroupAcc::getDoubleAccumulatorExtensions);
                put(COUNT_DISTINCT, FieldValueProfileGroupAcc::getCountDistinctAccumulatorExtensions);
                put(FIRST_SEEN, FieldValueProfileGroupAcc::getLongAccumulatorExtensions);
            }};

    List<String> keyFieldValues = new ArrayList<>();

    public FieldValueProfileGroupAcc(ProfileGroupConfig profileGroupConfig) {
        super(profileGroupConfig.getMeasurements().stream().
                map(config -> accFactory.get(config.getAggregationMethod()).get()).
                collect(Collectors.toList()));
    }

    @Override
    protected void updateAccumulators(Message message, ProfileGroupConfig profileGroupConfig) {
        Map<String, String> extensions = message.getExtensions();
        if (keyFieldValues.isEmpty()) {
            keyFieldValues = profileGroupConfig.getKeyFieldNames().stream().map(extensions::get).collect(Collectors.toList());
        }

        Iterator<ProfileMeasurementConfig> measurementConfigIter = profileGroupConfig.getMeasurements().iterator();
        Iterator<Accumulator<?, ? extends Serializable>> accumulatorIter = accumulators.iterator();
        while (measurementConfigIter.hasNext() && accumulatorIter.hasNext()) {
            ProfileMeasurementConfig measurementConfig = measurementConfigIter.next();
            Accumulator<?, ? extends Serializable> accumulator = accumulatorIter.next();
            accUpdate.get(measurementConfig.getAggregationMethod()).update(measurementConfig, message, accumulator);
        }

    }

    @Override
    protected void addExtensions(ProfileGroupConfig profileGroupConfig, Map<String, String> extensions, Map<String, DecimalFormat> measurementFormats) {
        Iterator<String> keyFieldNameIter = profileGroupConfig.getKeyFieldNames().iterator();
        Iterator<String> keyFieldValueIter = keyFieldValues.iterator();
        while(keyFieldNameIter.hasNext() && keyFieldValueIter.hasNext()) {
            extensions.put(keyFieldNameIter.next(), keyFieldValueIter.next());
        }
        Iterator<Accumulator<?, ? extends Serializable>> myAccIter = accumulators.iterator();
        Iterator<ProfileMeasurementConfig> measurementIter = profileGroupConfig.getMeasurements().iterator();
        while(myAccIter.hasNext() && measurementIter.hasNext()) {
            Accumulator<?, ? extends Serializable> acc = myAccIter.next();
            ProfileMeasurementConfig measurementConfig = measurementIter.next();
            if (acc != null) {
                DecimalFormat measurementFormat = measurementFormats.get(measurementConfig.getResultExtensionName());
                extensionUpdate.get(measurementConfig.getAggregationMethod()).update(measurementConfig, acc, extensions, measurementFormat);
            }
        }
    }

    @Override
    public void merge(ProfileGroupAcc other) {
        if (other instanceof FieldValueProfileGroupAcc) {
            if (keyFieldValues.isEmpty()) {
                keyFieldValues.addAll(((FieldValueProfileGroupAcc)other).keyFieldValues);
            }
        }
        super.merge(other);
    }

    private static void getDoubleAccumulatorExtensions(ProfileMeasurementConfig config, Accumulator<?, ? extends Serializable> acc, Map<String, String> extensions, DecimalFormat format) {
        extensions.put(config.getResultExtensionName(), format.format(((Accumulator<?, Double>)acc).getLocalValue()));
    }

    private static void getLongAccumulatorExtensions(ProfileMeasurementConfig config, Accumulator<?, ? extends Serializable> acc, Map<String, String> extensions, DecimalFormat format) {
        extensions.put(config.getResultExtensionName(), format.format(((Accumulator<?, Long>)acc).getLocalValue()));
    }

    private static void getCountDistinctAccumulatorExtensions(ProfileMeasurementConfig config, Accumulator<?, ? extends Serializable> acc, Map<String, String> extensions, DecimalFormat format) {
        extensions.put(config.getResultExtensionName(), format.format(((CountDistinctAcc)acc).getLocalValue().getUnion().getResult().getEstimate()));
    }

    private static  void updateDoubleAccumulator(ProfileMeasurementConfig config, Message message, Accumulator<?, ? extends Serializable> acc) {
        Double fieldValueDouble = getFieldValueAsDouble(message, config.getFieldName());
        if (fieldValueDouble != null) {
            ((Accumulator<Double, Double>) acc).add(fieldValueDouble);
        }
    }

    private static void updateCounterAccumulator(ProfileMeasurementConfig config, Message message, Accumulator<?, ? extends Serializable> acc) {
        ((Accumulator<Double, ? extends Serializable>)acc).add(1D);
    }

    private static void updateStringAccumulator(ProfileMeasurementConfig config, Message message, Accumulator<?, ? extends Serializable> acc) {
        String stringFieldValue = message.getExtensions().get(config.getFieldName());
        if (stringFieldValue != null) {
            ((Accumulator<String, ? extends Serializable>)acc).add(stringFieldValue);
        }
    }

    private static  void updateTimestampAccumulator(ProfileMeasurementConfig config, Message message, Accumulator<?, ? extends Serializable> acc) {
        ((Accumulator<Long, ? extends Serializable>)acc).add(message.getTs());
    }
}
