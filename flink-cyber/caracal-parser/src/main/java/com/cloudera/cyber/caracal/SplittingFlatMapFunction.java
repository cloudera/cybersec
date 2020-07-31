package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.google.common.collect.Streams;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.util.Collector;

import javax.json.Json;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Splits up JSON array based on a configured path and dupes 'header' in to multiple messages
 */
public class SplittingFlatMapFunction extends RichFlatMapFunction<String, Message> {

    private final JsonPath headerPath;
    private final String tsField;
    private final TimestampSource timestampSource;

    public enum TimestampSource {
        HEADER, SPLIT
    }
    private JsonPath jsonPath;

    private static final Configuration STRICT_PROVIDER_CONFIGURATION = Configuration.builder().jsonProvider(new JacksonJsonProvider()).build();

    public SplittingFlatMapFunction(String path, String headerPath, String tsField, TimestampSource timestampSource) {
        this.jsonPath = JsonPath.compile(path);
        this.headerPath = JsonPath.compile(headerPath);
        this.tsField = tsField;
        this.timestampSource = timestampSource;
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

        long headerTs = 1;

        ((List) jsonPathResult).forEach(part -> {
            long ts = timestampSource == TimestampSource.SPLIT ? 0 : headerTs;
            collector.collect(Message.builder().originalSource(s).ts(ts).fields(childPlusHeader((Map<String, Object>) part, headerFields)).build());
        });
        
    }

    private boolean isScalar(Object value) {
        return !(value instanceof Map || value instanceof List);
    }

    private Map<String, Object> childPlusHeader(Map<String, Object> part, Map<String,Object> header) {
        return Streams.concat(header.entrySet().stream(), part.entrySet().stream())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue, (a, b) -> b));
    }

    private Map<String,String> mapStringObjectToString(Map<String, Object> part) {
        return part.entrySet().stream().collect(Collectors.toMap(Map.Entry::getKey, v -> v.getValue().toString()));
    }
}
