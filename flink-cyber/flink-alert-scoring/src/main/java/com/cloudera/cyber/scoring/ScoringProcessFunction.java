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

import static com.cloudera.cyber.scoring.ScoringRule.RESULT_REASON;
import static com.cloudera.cyber.scoring.ScoringRule.RESULT_SCORE;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.DynamicRuleProcessFunction;
import com.cloudera.cyber.rules.RulesForm;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;
import org.apache.flink.util.Preconditions;

@Slf4j
public class ScoringProcessFunction
      extends DynamicRuleProcessFunction<ScoringRule, ScoringRuleCommand, ScoringRuleCommandResult, ScoredMessage> {

    private final String scoringSummarizationModeName;
    private transient ScoringSummarizationMode scoringSummarizationMode;
    public static final String ILLEGAL_SUMMARIZATION_METHOD_ERROR_MESSAGE =
          "Summarization method '%s' is not a legal value '%s'";

    public ScoringProcessFunction(OutputTag<ScoringRuleCommandResult> rulesResultSink,
                                  MapStateDescriptor<RulesForm, List<ScoringRule>> rulesDescriptor,
                                  ParameterTool params) {
        super(rulesResultSink, rulesDescriptor);
        this.scoringSummarizationModeName =
              params.get(ScoringJobKafka.scoringSummationName(), ScoringSummarizationMode.defaultValue().name());

        Preconditions.checkArgument(
              ScoringSummarizationMode.isLegalSummarizationMode(this.scoringSummarizationModeName),
              String.format(ILLEGAL_SUMMARIZATION_METHOD_ERROR_MESSAGE, this.scoringSummarizationModeName,
                    ScoringSummarizationMode.legalSummarizationModes()));

    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        setResultBuilderSupplier(ScoringRuleCommandResult::builder);
        scoringSummarizationMode = ScoringSummarizationMode.valueOf(scoringSummarizationModeName);
    }

    protected void processMessages(Message message, Collector<ScoredMessage> collector, List<ScoringRule> rules) {

        List<Scores> scores = Collections.emptyList();
        if (rules != null && !rules.isEmpty()) {
            scores = rules.stream()
                  .map(r -> {
                      Map<String, Object> results = r.apply(message);
                      if (results != null) {
                          Object score = results.get(RESULT_SCORE);
                          if (!(score instanceof Double)) {
                              score = new Double(String.valueOf(score));
                          }
                          return Scores.builder()
                                .ruleId(r.getId())
                                .score((Double) score)
                                .reason((String) results.get(RESULT_REASON))
                                .build();
                      } else {
                          return null;
                      }
                  })
                  .filter(Objects::nonNull)
                  .collect(Collectors.toList());

            //TODO remove if not needed. Was unused, which resulted to additional calculations for each message
            //DoubleSummaryStatistics scoreSummary = scores.stream().collect(Collectors.summarizingDouble(s -> s.getScore()));
        }
        if (log.isDebugEnabled()) {
            log.debug("Scored Message: {}, {}", message, rules);
        }

        collector.collect(scoreMessage(message, scores, scoringSummarizationMode));
    }

    public static ScoredMessage scoreMessage(Message message, List<Scores> scores,
                                             ScoringSummarizationMode summarizationMode) {
        final List<Double> scoreValues = scores.stream().map(Scores::getScore).collect(Collectors.toList());
        return ScoredMessage.builder()
              .message(message)
              .cyberScoresDetails(scores)
              .cyberScore(summarizationMode.calculateScore(scoreValues))
              .build();
    }

}
