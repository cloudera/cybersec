package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import com.cloudera.cyber.rules.DynamicRuleProcessFunction;
import com.cloudera.cyber.rules.RulesForm;
import lombok.extern.java.Log;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import static com.cloudera.cyber.scoring.ScoringRule.RESULT_REASON;
import static com.cloudera.cyber.scoring.ScoringRule.RESULT_SCORE;

@Log
public class ScoringProcessFunction extends DynamicRuleProcessFunction<ScoringRule, ScoringRuleCommand, ScoredMessage> {

    public ScoringProcessFunction(OutputTag<DynamicRuleCommandResult<ScoringRule>> rulesResultSink, MapStateDescriptor<RulesForm, List<ScoringRule>> rulesDescriptor) {
        super(rulesResultSink, rulesDescriptor);
    }

    protected void processMessages(Message message, Collector<ScoredMessage> collector, List<ScoringRule> rules) {
        if (rules == null || rules.size() == 0)
            collector.collect(ScoredMessage.builder().message(message).build());
        List<Scores> scores = rules.stream()
                .map(r -> {
                    Map<String, Object> results = r.apply(message);
                    return Scores.builder()
                            .ruleId(r.getId())
                            .score((Double) results.get(RESULT_SCORE))
                            .reason((String) results.get(RESULT_REASON))
                            .build();
                }).collect(Collectors.toList());

        log.info(String.format("%d, %s, %s", Thread.currentThread().getId(), message, rules));

        collector.collect(ScoredMessage.builder()
                .message(message)
                .scores(scores)
                .rules(rules)
                .build());
    }
}
