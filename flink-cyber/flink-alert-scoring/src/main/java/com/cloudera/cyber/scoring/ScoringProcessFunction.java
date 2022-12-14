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

package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.DynamicRuleProcessFunction;
import com.cloudera.cyber.rules.RulesForm;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.Collections;
import java.util.DoubleSummaryStatistics;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.cloudera.cyber.scoring.ScoringRule.RESULT_REASON;
import static com.cloudera.cyber.scoring.ScoringRule.RESULT_SCORE;

@Slf4j
public class ScoringProcessFunction extends DynamicRuleProcessFunction<ScoringRule, ScoringRuleCommand, ScoringRuleCommandResult, ScoredMessage> {

    public ScoringProcessFunction(OutputTag<ScoringRuleCommandResult> rulesResultSink, MapStateDescriptor<RulesForm, List<ScoringRule>> rulesDescriptor) {
        super(rulesResultSink, rulesDescriptor);
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

            DoubleSummaryStatistics scoreSummary = scores.stream().collect(Collectors.summarizingDouble(s -> s.getScore()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Scored Message: {}, {}", message, rules);
        }
        collector.collect(ScoredMessage.builder()
                .message(message)
                .cyberScoresDetails(scores)
                .build());
    }
}
