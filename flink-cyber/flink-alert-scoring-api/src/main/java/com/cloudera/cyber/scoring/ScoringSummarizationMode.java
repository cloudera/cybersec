package com.cloudera.cyber.scoring;

import org.apache.commons.collections.CollectionUtils;

import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.function.Function;

public enum ScoringSummarizationMode {
    SUM(ScoringSummarizationMode::sum),
    MAX(ScoringSummarizationMode::max),
    MEAN(ScoringSummarizationMode::mean),
    POSITIVE_MEAN(ScoringSummarizationMode::positiveMean);

    private final Function<List<Double>, Double> scoreFunction;

    ScoringSummarizationMode(Function<List<Double>, Double> scoreFunction) {
        this.scoreFunction = scoreFunction;
    }

    public static ScoringSummarizationMode DEFAULT() {
        return POSITIVE_MEAN;
    }

    public Double calculateScore(List<Double> scoreList) {
        if (CollectionUtils.isEmpty(scoreList)) {
            return 0.0;
        }
        return isCyberAlert(scoreList) ? scoreFunction.apply(scoreList) : 0.0;
    }

    private DoubleSummaryStatistics getSummaryPositive(List<Double> scoreList) {
        return scoreList.stream().mapToDouble(Double::doubleValue).filter(d -> d > 0).summaryStatistics();
    }

    private DoubleSummaryStatistics getSummaryNegative(List<Double> scoreList) {
        return scoreList.stream().mapToDouble(Double::doubleValue).filter(d -> d < 0).summaryStatistics();
    }

    private boolean isCyberAlert(List<Double> scoreList) {
        return (getSummaryPositive(scoreList).getAverage() > -getSummaryNegative(scoreList).getAverage());
    }

    private static double sum(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).sum();
    }

    private static double max(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).max().orElse(0L);
    }

    private static double mean(List<Double> list) {
        return list.stream().mapToDouble(Double::doubleValue).average().orElse(0L);
    }

    private static double positiveMean(List<Double> list) {
        double ret = 0d;
        int num = 0;
        for (Double n : list) {
            if (n > 0) {
                ret += n;
                num++;
            }
        }
        return num > 0 ? ret / num : 0d;
    }
}
