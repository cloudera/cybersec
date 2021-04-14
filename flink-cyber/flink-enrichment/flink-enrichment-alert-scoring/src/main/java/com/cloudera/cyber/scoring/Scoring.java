package com.cloudera.cyber.scoring;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import com.cloudera.cyber.rules.RulesForm;
import org.apache.flink.api.common.state.MapStateDescriptor;
import org.apache.flink.streaming.api.datastream.BroadcastStream;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.util.OutputTag;

import java.util.List;

public class Scoring {
    public static SingleOutputStreamOperator<ScoredMessage> score(DataStream<Message> data,
                                                                  DataStream<ScoringRuleCommand> ruleCommands,
                                                                  OutputTag<ScoringRuleCommandResult> rulesResultSink,
                                                                  MapStateDescriptor<RulesForm, List<ScoringRule>> rulesState) {
        BroadcastStream<ScoringRuleCommand> rulesStream = ruleCommands.broadcast(rulesState);
        return data.connect(rulesStream).process(new ScoringProcessFunction(rulesResultSink, rulesState));
    }
}
