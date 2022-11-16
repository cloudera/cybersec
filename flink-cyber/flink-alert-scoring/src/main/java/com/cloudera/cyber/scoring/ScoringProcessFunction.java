package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.DynamicRuleProcessFunction;
import com.cloudera.cyber.rules.RulesForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.scoring.ScoringRule.RESULT_REASON;
import static com.cloudera.cyber.scoring.ScoringRule.RESULT_SCORE;

@Slf4j
public class ScoringProcessFunction extends DynamicRuleProcessFunction<ScoringRule, ScoringRuleCommand, ScoringRuleCommandResult, ScoredMessage> {

    private final ParameterTool params;

    public ScoringProcessFunction(OutputTag<ScoringRuleCommandResult> rulesResultSink, MapStateDescriptor<RulesForm, List<ScoringRule>> rulesDescriptor, ParameterTool params) {
        super(rulesResultSink, rulesDescriptor);
        this.params = params;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        setResultBuilderSupplier(ScoringRuleCommandResult::builder);
    }

    protected void processMessages(Message message, Collector<ScoredMessage> collector, List<ScoringRule> rules) {

        List<Scores> scores = Collections.emptyList();
        if (rules != null && !rules.isEmpty()) {
            scores = rules.stream()
                    .map(r -> {
                        Map<String, Object> results = r.apply(message);
                        if (results != null) {
                            return Scores.builder()
                                    .ruleId(r.getId())
                                    .score((Double) results.get(RESULT_SCORE))
                                    .reason((String) results.get(RESULT_REASON))
                                    .build();
                        } else {
                            return null;
                        }
                    })
                    .filter(f -> f != null)
                    .collect(Collectors.toList());

            //TODO remove if not needed. Was unused, which resulted to additional calculations for each message
            //DoubleSummaryStatistics scoreSummary = scores.stream().collect(Collectors.summarizingDouble(s -> s.getScore()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Scored Message: {}, {}", message, rules);
        }
        final List<Double> scoreValues = scores.stream().map(Scores::getScore).collect(Collectors.toList());
        collector.collect(ScoredMessage.builder()
                .message(message)
                .cyberScoresDetails(scores)
                .cyberScore(getScoringSummarizationMode(params).calculateScore(scoreValues))
                .build());
    }

    protected static ScoringSummarizationMode getScoringSummarizationMode(ParameterTool params) {
        return ScoringSummarizationMode.valueOf(
                params.get(ScoringJobKafka.SCORING_SUMMATION_NAME(), ScoringSummarizationMode.DEFAULT().name()));
    }
}
