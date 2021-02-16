package com.cloudera.cyber.profiler.accumulator;

import com.cloudera.cyber.Message;
import com.google.common.collect.Lists;
import org.apache.commons.math3.stat.descriptive.AggregateSummaryStatistics;
import org.apache.commons.math3.stat.descriptive.StatisticalSummaryValues;
import org.apache.commons.math3.stat.descriptive.SummaryStatistics;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class StatsProfileAccumulator extends ProfileAccumulator {
    private static final DecimalFormat DEFAULT_FORMAT = new DecimalFormat("0.00");
    public static final String MIN_RESULT_SUFFIX = ".min";
    public static final String MAX_RESULT_SUFFIX = ".max";
    public static final String MEAN_RESULT_SUFFIX = ".mean";
    public static final String STDDEV_RESULT_SUFFIX = ".stddev";
    public static final String STATS_PROFILE_GROUP_SUFFIX = ".stats";
    public static List<String> STATS_EXTENSION_SUFFIXES = Lists.newArrayList(MIN_RESULT_SUFFIX, MAX_RESULT_SUFFIX, MEAN_RESULT_SUFFIX, STDDEV_RESULT_SUFFIX);
    private final List<SummaryStatistics> mergedStatistics = new ArrayList<>();
    private final DecimalFormat format;
    private final String fieldName;

    public StatsProfileAccumulator(String resultExtensionName, String extensionName, DecimalFormat format) {
        super(resultExtensionName);
        this.format = (format != null) ? format : DEFAULT_FORMAT;
        this.fieldName = extensionName;
        this.mergedStatistics.add(new SummaryStatistics());
    }

    @Override
    public void add(Message message) {
        Double fieldValue = getFieldValueAsDouble(message);
        if (fieldValue != null) {
            mergedStatistics.get(0).addValue(fieldValue);
        }
    }

    @Override
    public void addResults(Map<String, String> results) {
        StatisticalSummaryValues stats = AggregateSummaryStatistics.aggregate(mergedStatistics);
        String resultExtensionName = getResultExtensionName();
        results.put(resultExtensionName.concat(MIN_RESULT_SUFFIX), format.format(stats.getMin()));
        results.put(resultExtensionName.concat(MAX_RESULT_SUFFIX), format.format(stats.getMax()));
        results.put(resultExtensionName.concat(MEAN_RESULT_SUFFIX), format.format(stats.getMean()));
        results.put(resultExtensionName.concat(STDDEV_RESULT_SUFFIX), format.format(stats.getStandardDeviation()));
    }

    @Override
    public void merge(ProfileAccumulator other) {
        if (this != other && other instanceof StatsProfileAccumulator ) {
            mergedStatistics.addAll(((StatsProfileAccumulator)other).mergedStatistics);
        }
    }

    protected Double getFieldValueAsDouble(Message message) {
        String extensionValue = message.getExtensions().get(fieldName);
        if (extensionValue != null) {
            try {
                return Double.parseDouble(extensionValue);
            } catch (NumberFormatException ignored){
                // extension value is not numeric so we cant do stats on it
            }
        }
        return null;
    }

    public static boolean isStatsExtension(String extensionName) {
        int suffixStart = extensionName.lastIndexOf('.');
        if (suffixStart > 0) {
            return STATS_EXTENSION_SUFFIXES.contains(extensionName.substring(suffixStart));
        }
        return false;
    }

}
