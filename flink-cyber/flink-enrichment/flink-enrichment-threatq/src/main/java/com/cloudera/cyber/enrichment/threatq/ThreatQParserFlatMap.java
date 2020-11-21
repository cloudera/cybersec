package com.cloudera.cyber.enrichment.threatq;

import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.util.Collector;

import java.io.ByteArrayInputStream;

public class ThreatQParserFlatMap implements FlatMapFunction<String, ThreatQEntry> {
    @Override
    public void flatMap(String s, Collector<ThreatQEntry> collector) throws Exception {
        ThreatQParser.parse(new ByteArrayInputStream(s.getBytes())).forEach(out -> collector.collect(out));
    }
}
