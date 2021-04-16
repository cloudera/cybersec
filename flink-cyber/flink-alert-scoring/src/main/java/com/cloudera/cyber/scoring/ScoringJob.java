package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.rules.RulesForm;
import java.io.IOException;
import org.apache.flink.annotation.VisibleForTesting;
import org.apache.flink.api.common.state.ListStateDescriptor;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.avro.typeutils.AvroTypeInfo;
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

    private static final String RULE_STATE_TAG = "rules";
    private static final String RULE_COMMANDS_TAG = "rules-sink";
    public static final OutputTag<ScoringRuleCommandResult> COMMAND_RESULT_OUTPUT_TAG = new OutputTag<>(RULE_COMMANDS_TAG, TypeInformation.of(ScoringRuleCommandResult.class));

    protected StreamExecutionEnvironment createPipeline(final ParameterTool params) {
        final StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);
        FlinkUtils.setupEnv(env, params);

        final DataStream<ScoringRuleCommand> ruleCommands = createRulesSource(env, params);
        final DataStream<Message> data = createSource(env, params);

        final SingleOutputStreamOperator<ScoredMessage> results = Scoring
                .score(data, ruleCommands, Descriptors.rulesResultSink, Descriptors.rulesState).name("Process Rules")
                .uid("process-rules");

        writeResults(params, results);
        // output the results of the rules commands
        writeQueryResult(params, results.getSideOutput(Descriptors.rulesResultSink));

        return env;
    }

    public static SingleOutputStreamOperator<ScoredMessage> enrich(DataStream<Message> source,  DataStream<ScoringRuleCommand> ruleCommands) {
        return Scoring
                .score(source, ruleCommands, Descriptors.rulesResultSink, Descriptors.rulesState).name("Process Rules")
                .uid("process-rules");
    }

    @VisibleForTesting
    static class Descriptors {
        public static final MapStateDescriptor<RulesForm, List<ScoringRule>> rulesState =
                new MapStateDescriptor<>(
                        RULE_STATE_TAG, TypeInformation.of(RulesForm.class), Types.LIST(new AvroTypeInfo<>(ScoringRule.class)));

        public static final OutputTag<ScoringRuleCommandResult> rulesResultSink =
                new OutputTag<ScoringRuleCommandResult>(RULE_COMMANDS_TAG) {
                };
        public static ListStateDescriptor<ScoringRule> activeOrderedRules;
    }

    protected abstract void writeResults(ParameterTool params, DataStream<ScoredMessage> results);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<ScoringRuleCommand> createRulesSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract void writeQueryResult(ParameterTool params, DataStream<ScoringRuleCommandResult> results);
}
