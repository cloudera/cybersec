package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.*;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Slf4j
public abstract class DynamicRuleProcessFunction<R extends DynamicRule<R>, C extends DynamicRuleCommand<R>, CR extends DynamicRuleCommandResult<R> , T>
        extends BroadcastProcessFunction<Message, C, T> {
    @NonNull
    protected OutputTag<CR> outputSink;
    @NonNull
    protected MapStateDescriptor<RulesForm, List<R>> rulesStateDescriptor;
    protected transient Supplier<DynamicRuleCommandResult.DynamicRuleCommandResultBuilder<R, CR>> resultBuilderSupplier;

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<T> collector) throws Exception {
        log.debug("{}, time: {}, processing: {}, watermark: {}: {}",Thread.currentThread().getId(), readOnlyContext.timestamp(), readOnlyContext.currentProcessingTime(), readOnlyContext.currentWatermark(), message);
        List<R> rules = readOnlyContext.getBroadcastState(rulesStateDescriptor).get(RulesForm.ACTIVE);
        processMessages(message, collector, rules);
    }

    protected void setResultBuilderSupplier(Supplier<DynamicRuleCommandResult.DynamicRuleCommandResultBuilder<R, CR>> resultBuilderSupplier) {
        this.resultBuilderSupplier = resultBuilderSupplier;
    }

    protected abstract void processMessages(Message message, Collector<T> collector, List<R> rules);

    @Override
    public void processBroadcastElement(C dynamicRuleCommand, Context context, Collector<T> collector) throws Exception {
        log.debug("Rule Command {}, time: {}, processing: %{}, watermark: {}: {}" , Thread.currentThread().getId(), context.timestamp(), context.currentProcessingTime(), context.currentWatermark(), dynamicRuleCommand);
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
    }

    private List<R> processRulesCommands(List<R> stateRules, C ruleCommand, Context context) {
        List<R> rules = stateRules == null ? Collections.emptyList() : stateRules;
        log.debug("Processing Rule Command {}", ruleCommand);
        switch (ruleCommand.getType()) {
            case DELETE:
                outputRule(context, null, ruleCommand.getId());
                return rules.stream()
                        .filter(r -> !r.getId().equals(ruleCommand.getRuleId()))
                        .collect(Collectors.toList());
            case UPSERT:
                String ruleId = (ruleCommand.getRule().getId() == null) ? UUID.randomUUID().toString() : ruleCommand.getRule().getId();
                Optional<R> matchingRule = rules.stream().filter(r -> r.getId().equals(ruleId)).findFirst();
                int newRuleVersion = 0;
                if (matchingRule.isPresent()) {
                    newRuleVersion = matchingRule.get().getVersion() + 1;
                }

                R newRule = ruleCommand.getRule().withVersion(newRuleVersion).withId(ruleId);
                outputRule(context, newRule, ruleCommand.getId());
                return Stream.concat(rules.stream().filter(r -> !r.getId().equals(ruleCommand.getRuleId())),
                                Stream.of(newRule))
                                .collect(Collectors.toList());
            case ENABLE:
                return setEnable(context, ruleCommand.getId(), rules, ruleCommand.getRuleId(), true);
            case DISABLE:
                return setEnable(context, ruleCommand.getId(), rules, ruleCommand.getRuleId(), false);
            case GET:
                R getRuleResult = rules.stream().filter(r -> r.getId().equals(ruleCommand.getRuleId())).findFirst().orElse(null);
                outputRule(context, getRuleResult, ruleCommand.getId());
                return rules;
            case LIST:
                rules.forEach(r -> outputRule(context, r, ruleCommand.getId()));
                // indicate end of rules with empty rule or just empty rule if there are no rules loaded yet
                outputRule(context, null, ruleCommand.getId());
                return rules;
            default:
                throw new IllegalStateException("Unkonwn rule type sent");
        }
    }

    private R outputRule(Context context, R r, String cmdId) {
        context.output(this.outputSink, resultBuilderSupplier.get()
                .cmdId(cmdId)
                .rule(r)
                .success(true)
                .build());
        return r;
    }

    private List<R> setEnable(Context context, String cmdId, List<R> allScoringRules, String ruleId, boolean state) {
        return allScoringRules.stream()
                .map(r -> {
                    if (r.getId().equals(ruleId)) {
                        R newRule = r.withEnabled(state);
                        outputRule(context, newRule, cmdId);
                        return newRule;
                    } else {
                        return r;
                    }
                })
                .collect(Collectors.toList());
    }
}
