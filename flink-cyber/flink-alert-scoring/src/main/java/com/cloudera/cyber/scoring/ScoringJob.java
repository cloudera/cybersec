package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.rules.RulesForm;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.OutputTag;

import java.util.List;

/**
 * A job that will score alerts based on a set of dynamic rules stored in the state engine
 */
public abstract class ScoringJob {
    protected StreamExecutionEnvironment createPipeline(final ParameterTool params) {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        final DataStream<ScoringRuleCommand> ruleCommands = createRulesSource(env, params);
        final DataStream<Message> data = createSource(env, params);

        final SingleOutputStreamOperator<ScoredMessage> results = Scoring
                .score(data, ruleCommands, Descriptors.rulesResultSink, Descriptors.rulesState).name("Process Rules")
                .uid("process-rules");

        // output the results of the rules commands
        final DataStream<ScoringRuleCommandResult> rulesCommandResponse =
                results.getSideOutput(Descriptors.rulesResultSink);

        writeResults(params, results);
        writeQueryResult(params, rulesCommandResponse);

        return env;
    }

    @VisibleForTesting
    static class Descriptors {
        public static final MapStateDescriptor<RulesForm, List<ScoringRule>> rulesState =
                new MapStateDescriptor<>(
                        "rules", TypeInformation.of(RulesForm.class), Types.LIST(TypeInformation.of(ScoringRule.class)));

        public static final OutputTag<ScoringRuleCommandResult> rulesResultSink =
                new OutputTag<ScoringRuleCommandResult>("rules-sink") {
                };
        public static ListStateDescriptor<ScoringRule> activeOrderedRules;
    }

    protected abstract void writeResults(ParameterTool params, DataStream<ScoredMessage> results);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeQueryResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results);
}
