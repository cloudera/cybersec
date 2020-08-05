package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.rules.engines.JavascriptEngineBuilder;
import com.cloudera.cyber.rules.engines.RuleEngine;
import com.google.common.collect.Streams;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;
import org.joda.time.Instant;

import javax.script.ScriptException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Splits up JSON array based on a configured path and dupes 'header' in to multiple messages
 */
public class SplittingFlatMapFunction extends RichFlatMapFunction<String, Message> {

    private final JsonPath headerPath;
    private final String tsField;
    private final TimestampSource timestampSource;
    private final MapFunction<String, Long> timestampFunction;
    private final RuleEngine engine;
    private final String topic;

    public enum TimestampSource {
        HEADER, SPLIT
    }
    private JsonPath jsonPath;

    private static final Configuration STRICT_PROVIDER_CONFIGURATION = Configuration.builder().jsonProvider(new JacksonJsonProvider()).build();

    public SplittingFlatMapFunction(SplitConfig config) {
        this.topic = config.getTopic();
        this.jsonPath = JsonPath.compile(config.getSplitPath());
        this.headerPath = JsonPath.compile(config.getHeaderPath());
        this.tsField = config.getTimestampField();
        this.timestampSource = config.getTimestampSource();

        if(config.getTimestampFunction() != null & !config.getTimestampFunction().isEmpty()) {
            this.engine = new JavascriptEngineBuilder()
                    .script("")
                    .build();

            try {
                this.engine.eval("function transform(ts) { return " + config.getTimestampFunction() + "}");
            } catch (ScriptException e) {
                throw new RuntimeException(e);
            }
            this.timestampFunction = (String s) -> {
                Double out = (Double) engine.invokeFunction("transform", s);
                return out.longValue();
            };
        } else {
            this.engine = null;
            this.timestampFunction = (String s) -> {
                return Long.valueOf(s);
            };
        }

    }

    @Override
    public void flatMap(String s, Collector<Message> collector) throws Exception {
        DocumentContext documentContext = JsonPath.using(STRICT_PROVIDER_CONFIGURATION).parse(s);

        // header is all top level simple type fields.
        Object header = documentContext.read(headerPath);
        new HashMap<>();
        Map<String, Object> headerFields = header instanceof Map ?
                ((Map<String, Object>) header).entrySet().stream()
                    .filter(e -> isScalar(e.getValue()))
                    .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)) :
                Collections.emptyMap();

        Object jsonPathResult = documentContext.read(jsonPath);

        List resultList = (List) jsonPathResult;

        ((List) jsonPathResult).forEach(part -> {
            Map<String, Object> fields = (Map<String, Object>) part;


            String tsString = (String) (timestampSource == TimestampSource.SPLIT ?
                                fields.get(tsField) :
                                headerFields.get(tsField));

            long ts;
            try {
                ts = Long.valueOf(tsString);
            } catch (NumberFormatException e) {
                try {
                    ts = this.timestampFunction.map(tsString);
                } catch (Exception ex) {
                    throw new RuntimeException("Timestamp script failed", ex);
                }
            }

            collector.collect(Message.newBuilder()
                    .setId(UUID.randomUUID().toString())
                    .setOriginalSource(s)
                    .setSource(this.topic)
                    .setTs(Instant.ofEpochMilli(ts).toDateTime())
                    .setExtensions(childPlusHeader(topic, fields, headerFields)).build());
        });
        
    }

    private boolean isScalar(Object value) {
        return !(value instanceof Map || value instanceof List);
    }

    private Map<String, Object> childPlusHeader(String topic, Map<String, Object> part, Map<String, Object> header) {
        Map<String, Object> result = Streams.concat(header.entrySet().stream(), part.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
        result.put("source", topic);
        return result;
    }

    private Map<String,String> mapStringObjectToString(Map<String, Object> part) {
        return part.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toString()));
    }
}
