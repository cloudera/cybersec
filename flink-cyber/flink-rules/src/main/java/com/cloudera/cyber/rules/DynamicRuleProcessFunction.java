package com.cloudera.cyber.rules;

import com.cloudera.cyber.Message;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import lombok.extern.java.Log;
import org.apache.flink.api.common.state.BroadcastState;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.functions.co.BroadcastProcessFunction;
import org.apache.flink.util.Collector;
import org.apache.flink.util.OutputTag;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@RequiredArgsConstructor
@Log
public abstract class DynamicRuleProcessFunction<R extends DynamicRule, C extends DynamicRuleCommand<R>, T>
        extends BroadcastProcessFunction<Message, C, T> {
    @NonNull
    protected OutputTag<DynamicRuleCommandResult<R>> outputSink;
    @NonNull
    protected MapStateDescriptor<RulesForm, List<R>> rulesStateDescriptor;

    @Override
    public void processElement(Message message, ReadOnlyContext readOnlyContext, Collector<T> collector) throws Exception {
        log.info(String.format("%d, time: %d, processing: %d, watermark: %d: %s",Thread.currentThread().getId(), readOnlyContext.timestamp(), readOnlyContext.currentProcessingTime(), readOnlyContext.currentWatermark(), message));
        List<R> rules = readOnlyContext.getBroadcastState(rulesStateDescriptor).get(RulesForm.ACTIVE);
        processMessages(message, collector, rules);
    }

    protected abstract void processMessages(Message message, Collector<T> collector, List<R> rules);

    @Override
    public void processBroadcastElement(C dynamicRuleCommand, Context context, Collector<T> collector) throws Exception {
        log.info(String.format("%d, time: %d, processing: %d, watermark: %d: %s" , Thread.currentThread().getId(), context.timestamp(), context.currentProcessingTime(), context.currentWatermark(), dynamicRuleCommand));
        BroadcastState<RulesForm, List<R>> state = context.getBroadcastState(rulesStateDescriptor);
        List<R> scoringRules = processRulesCommands(state.get(RulesForm.ALL), dynamicRuleCommand, context);

        // update all rules
        state.put(RulesForm.ALL, scoringRules);

        List<R> activeRules = scoringRules.stream()
                .filter(r -> r.isEnabled())
                .sorted(Comparator.comparingInt(r -> r.getOrder()))
                .collect(Collectors.toList());

        // update the ordered version with just the active rules for processing
        state.put(RulesForm.ACTIVE, activeRules);
    }

    private List<R> processRulesCommands(List<R> stateRules, C ruleCommand, Context context) {
        List<R> rules = stateRules == null ? Collections.emptyList() : stateRules;
        switch (ruleCommand.getType()) {
            case DELETE:
                outputRule(context, null, ruleCommand.getId());
                return rules.stream()
                        .filter(r -> !r.getId().equals(ruleCommand.getRuleId()))
                        .collect(Collectors.toList());
            case UPSERT:
                R newRule = (ruleCommand.getRule().getId() == null) ?
                        (R) ruleCommand.getRule().withId(UUID.randomUUID()) :
                        ruleCommand.getRule();
                outputRule(context, newRule, ruleCommand.getId());
                return ruleCommand.getRuleId() == null ?
                        Stream.concat(rules.stream(),
                                Stream.of(newRule))
                                .collect(Collectors.toList()) :
                        Stream.concat(rules.stream().filter(r -> r.getId() != ruleCommand.getRuleId()),
                                Stream.of(newRule))
                                .collect(Collectors.toList());
            case ENABLE:
                return setEnable(rules, ruleCommand.getRuleId(), true);
            case DISABLE:
                return setEnable(rules, ruleCommand.getRuleId(), false);
            case GET:
                return rules.stream()
                        .filter(r -> r.getId() == ruleCommand.getRuleId())
                        .map(r -> outputRule(context, r, ruleCommand.getId()))
                        .collect(Collectors.toList());
            case LIST:
                return rules.stream()
                        .map(r -> outputRule(context, r, ruleCommand.getId()))
                        .collect(Collectors.toList());
            default:
                throw new IllegalStateException("Unkonwn rule type sent");
        }
    }

    private R outputRule(Context context, R r, UUID cmdId) {
        context.output(this.outputSink, DynamicRuleCommandResult.<R>builder()
                .cmdId(cmdId)
                .rule(r)
                .success(true)
                .build());
        return r;
    }

    private List<R> setEnable(List<R> allScoringRules, UUID ruleId, boolean state) {
        return allScoringRules.stream()
                .map(r -> r.getId() == ruleId ?
                        (R) r.withEnabled(state) :
                        (R) r)
                .collect(Collectors.toList());
    }
}
