package com.cloudera.cyber.ondemand.enrichment;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.commands.EnrichmentCommandResponse;
import com.cloudera.cyber.enrichment.EnrichmentJob;
import com.cloudera.cyber.flink.FlinkUtils;
import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.ondemand.config.OndemandEnrichmentConfiguration;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import com.cloudera.cyber.scoring.ScoringRuleCommandResult;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.util.Preconditions;

public class OnDemandEnrichmentJob extends EnrichmentJob {


    public static void main(String[] args) throws Exception {
        Preconditions.checkArgument(args.length >= 1, "Arguments must consist of a properties files");
        ParameterTool params = Utils.getParamToolsFromProperties(args);

        OndemandEnrichmentConfiguration configuration = OndemandEnrichmentConfiguration.readConfiguration(params);

        FlinkUtils.executeEnv(new OnDemandEnrichmentJob().createPipeline(params),"OnDemandTriage Job - default",params);
    }

    @Override
    protected org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator<Message> createSource(org.apache.flink.streaming.api.environment.StreamExecutionEnvironment env, ParameterTool params) {
        return null;
    }

    @Override
    protected void writeResults(org.apache.flink.streaming.api.environment.StreamExecutionEnvironment env, ParameterTool params, org.apache.flink.streaming.api.datastream.DataStream<ScoredMessage> results) {

    }

    @Override
    protected org.apache.flink.streaming.api.datastream.DataStream<EnrichmentCommand> createEnrichmentSource(org.apache.flink.streaming.api.environment.StreamExecutionEnvironment env, ParameterTool params) {
        return null;
    }

    @Override
    protected void writeEnrichmentQueryResults(org.apache.flink.streaming.api.environment.StreamExecutionEnvironment env, ParameterTool params, org.apache.flink.streaming.api.datastream.DataStream<EnrichmentCommandResponse> sideOutput) {

    }

    @Override
    protected org.apache.flink.streaming.api.datastream.DataStream<EnrichmentCommand> createThreatQSource(org.apache.flink.streaming.api.environment.StreamExecutionEnvironment env, ParameterTool params) {
        return null;
    }

    @Override
    protected org.apache.flink.streaming.api.datastream.DataStream<ScoringRuleCommand> createRulesSource(org.apache.flink.streaming.api.environment.StreamExecutionEnvironment env, ParameterTool params) {
        return null;
    }

    @Override
    protected void writeScoredRuleCommandResult(ParameterTool params, org.apache.flink.streaming.api.datastream.DataStream<ScoringRuleCommandResult> results) {

    }
}
