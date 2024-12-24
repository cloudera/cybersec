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

package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

@RequiredArgsConstructor
@Slf4j
public abstract class DynamicRuleProcessFunction<
      R extends DynamicRule<R>,
      C extends DynamicRuleCommand<R>,
      CR extends DynamicRuleCommandResult<R>, T>
      extends BroadcastProcessFunction<Message, C, T> {
    @NonNull
    protected OutputTag<CR> outputSink;
    @NonNull
    protected MapStateDescriptor<RulesForm, List<R>> rulesStateDescriptor;
    protected transient Supplier<DynamicRuleCommandResult.DynamicRuleCommandResultBuilder<R, CR>> resultBuilderSupplier;

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<T> collector)
          throws Exception {
        log.debug("{}, time: {}, processing: {}, watermark: {}: {}", Thread.currentThread().getId(),
              readOnlyContext.timestamp(), readOnlyContext.currentProcessingTime(), readOnlyContext.currentWatermark(),
              message);
        List<R> rules = readOnlyContext.getBroadcastState(rulesStateDescriptor).get(RulesForm.ACTIVE);
        processMessages(message, collector, rules);
    }

    protected void setResultBuilderSupplier(
          Supplier<DynamicRuleCommandResult.DynamicRuleCommandResultBuilder<R, CR>> resultBuilderSupplier) {
        this.resultBuilderSupplier = resultBuilderSupplier;
    }

    protected abstract void processMessages(Message message, Collector<T> collector, List<R> rules);

    @Override
    public void processBroadcastElement(C dynamicRuleCommand, Context context, Collector<T> collector)
          throws Exception {
        log.info("Rule Command {}, time: {}, processing: %{}, watermark: {}: {}", Thread.currentThread().getId(),
              context.timestamp(), context.currentProcessingTime(), context.currentWatermark(), dynamicRuleCommand);
        BroadcastState<RulesForm, List<R>> state = context.getBroadcastState(rulesStateDescriptor);
        List<R> scoringRules = processRulesCommands(state.get(RulesForm.ALL), dynamicRuleCommand, context);

        // update all rules
        state.put(RulesForm.ALL, scoringRules);

        List<R> activeRules = scoringRules.stream()
              .filter(DynamicRule::isEnabled)
              .sorted(Comparator.comparingInt(DynamicRule::getOrder))
              .collect(Collectors.toList());

        // update the ordered version with just the active rules for processing
        state.put(RulesForm.ACTIVE, activeRules);
        log.info("All Rules {}", scoringRules);
        log.info("Enabled Rules {}", activeRules);
    }

    private List<R> processRulesCommands(List<R> stateRules, C ruleCommand, Context context) {
        List<R> rules = stateRules == null ? Collections.emptyList() : stateRules;
        log.debug("Processing Rule Command {}", ruleCommand);
        switch (ruleCommand.getType()) {
            case DELETE:
                final List<R> newRules = rules.stream()
                      .filter(r -> {
                          if (r.getId().equals(ruleCommand.getRuleId())) {
                              log.info("Successfully removed rule with id '{}'.", ruleCommand.getRuleId());
                              outputRule(context, r, ruleCommand.getId());
                              return false;
                          } else {
                              return true;
                          }
                      })
                      .collect(Collectors.toList());
                if (newRules.size() == rules.size()) {
                    log.info("Unable to find rule with id '{}' to remove.", ruleCommand.getRuleId());
                    outputRule(context, null, ruleCommand.getId());
                }
                return newRules;
            case UPSERT:
                String ruleId = (ruleCommand.getRule().getId() == null) ? UUID.randomUUID().toString() :
                      ruleCommand.getRule().getId();
                Optional<R> matchingRule = rules.stream().filter(r -> r.getId().equals(ruleId)).findFirst();
                int newRuleVersion = 0;
                if (matchingRule.isPresent()) {
                    newRuleVersion = matchingRule.get().getVersion() + 1;
                    log.info("Rule with id '{}' has been updated.", ruleCommand.getRuleId());
                } else {
                    log.info("Rule with id '{}' has been created.", ruleCommand.getRuleId());
                }
                R newRule = ruleCommand.getRule().withVersion(newRuleVersion).withId(ruleId);

                Boolean success = newRule.isValid();

                outputRule(context, newRule, ruleCommand.getId(), success);
                if (success) {
                    return Stream.concat(rules.stream().filter(r -> !r.getId().equals(ruleCommand.getRuleId())),
                                Stream.of(newRule))
                          .collect(Collectors.toList());
                } else {
                    return rules;
                }

            case ENABLE:
                return setEnable(context, ruleCommand.getId(), rules, ruleCommand.getRuleId(), true);
            case DISABLE:
                return setEnable(context, ruleCommand.getId(), rules, ruleCommand.getRuleId(), false);
            case GET:
                R getRuleResult =
                      rules.stream().filter(r -> r.getId().equals(ruleCommand.getRuleId())).findFirst().orElse(null);
                log.info("Get rule with id '{}' ", ruleCommand.getRuleId());
                outputRule(context, getRuleResult, ruleCommand.getId());
                return rules;
            case LIST:
                log.info("Get all rules");
                rules.forEach(r -> outputRule(context, r, ruleCommand.getId()));
                // indicate end of rules with empty rule or just empty rule if there are no rules loaded yet
                outputRule(context, null, ruleCommand.getId());
                return rules;
            default:
                throw new IllegalStateException("Unkonwn rule type sent");
        }
    }

    private void outputRule(Context context, R r, String cmdId) {

        outputRule(context, r, cmdId, true);
    }

    private void outputRule(Context context, R r, String cmdId, Boolean success) {
        context.output(this.outputSink, resultBuilderSupplier.get()
              .cmdId(cmdId)
              .rule(r)
              .success(success)
              .subtaskNumber(getRuntimeContext().getIndexOfThisSubtask())
              .build());
    }

    private List<R> setEnable(Context context, String id, List<R> allScoringRules, String ruleId, boolean state) {
        boolean rulesNoInteraction = true;
        List<R> newScoringRules = new ArrayList<>(allScoringRules.size());
        for (R rule : allScoringRules) {
            if (rule.getId().equals(ruleId)) {
                final R newRule = rule.withEnabled(state);
                outputRule(context, newRule, id);
                rulesNoInteraction = false;
                newScoringRules.add(newRule);
                log.info("Rule with id '{}' was {}.", rule.getId(), state ? "enabled" : "disabled");
            } else {
                newScoringRules.add(rule);
            }
        }
        if (rulesNoInteraction) {
            log.info("Unable to find rule with id '{}' to {}.", ruleId, state ? "enable" : "disable");
            outputRule(context, null, id);
        }
        return newScoringRules;
    }
}
