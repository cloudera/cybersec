package com.cloudera.cyber.enrichment.stix.parsing;

import com.google.common.io.Resources;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;

import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;
import java.util.stream.Collectors;

public abstract class AbstractStixParserTest {

    protected List<ParsedThreatIntelligence> doTest(String file,
                                                  Consumer<ParsedThreatIntelligence> perMessageAssert,
                                                  Consumer<List<ParsedThreatIntelligence>> allAssert) throws Exception {
        URL url = Resources.getResource(file);
        String sample = Resources.toString(url, StandardCharsets.UTF_8);

        ConcurrentLinkedQueue<ParsedThreatIntelligence> q = new ConcurrentLinkedQueue<>();

        Collector<ParsedThreatIntelligence> collector = new Collector<ParsedThreatIntelligence>() {
            @Override
            public void collect(ParsedThreatIntelligence parsedThreatIntelligence) {
                q.add(parsedThreatIntelligence);
                perMessageAssert.accept(parsedThreatIntelligence);
            }

            @Override
            public void close() {
            }
        };

        Parser parser = new Parser();
        parser.open(new Configuration());
        parser.flatMap(sample, collector);

        List<ParsedThreatIntelligence> all = q.stream().collect(Collectors.toList());
        allAssert.accept(all);
        return all;
    };
}
