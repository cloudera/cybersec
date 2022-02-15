package com.cloudera.cyber.generator;

import com.cloudera.cyber.generator.ThreatGenerator;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

public class ThreatGeneratorMap extends RichMapFunction<String, Tuple2<String, String>> {
    private transient ThreatGenerator threatGenerator;

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        threatGenerator = new ThreatGenerator();
    }

    @Override
    public Tuple2<String, String> map(String ip) throws Exception {
        return Tuple2.of("threats", threatGenerator.generateThreat(ip));
    }
}
