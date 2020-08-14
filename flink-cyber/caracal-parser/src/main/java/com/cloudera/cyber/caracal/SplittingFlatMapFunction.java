package com.cloudera.cyber.caracal;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.SignedSourceKey;
import com.cloudera.cyber.parser.MessageToParse;
import com.cloudera.cyber.rules.engines.JavascriptEngineBuilder;
import com.cloudera.cyber.rules.engines.RuleEngine;
import com.cloudera.cyber.sha1;
import com.google.common.collect.Streams;
import com.jayway.jsonpath.DocumentContext;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.spi.json.JacksonJsonProvider;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.common.functions.RichFlatMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.util.Collector;
import org.apache.flink.util.Preconditions;

import javax.script.ScriptException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.Signature;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Splits up JSON array based on a configured path and dupes 'header' in to multiple messages
 */
@Slf4j
public class SplittingFlatMapFunction extends RichFlatMapFunction<MessageToParse, Message> {
    @NonNull
    private final SplitConfig config;
    @NonNull
    private final PrivateKey signKey;

    private transient String topic;
    private transient JsonPath headerPath;
    private transient String tsField;
    private transient TimestampSource timestampSource;

    private transient MapFunction<String, Long> timestampFunction;
    private transient RuleEngine engine;

    private transient Signature signature;

    public enum TimestampSource {
        HEADER, SPLIT
    }
    private JsonPath jsonPath;

    private static final com.jayway.jsonpath.Configuration STRICT_PROVIDER_CONFIGURATION =
            com.jayway.jsonpath.Configuration.builder().jsonProvider(new JacksonJsonProvider()).build();

    public SplittingFlatMapFunction(SplitConfig config, PrivateKey signKey) {
        Preconditions.checkNotNull(signKey, "Must supply signing key");
        Preconditions.checkNotNull(config, "Must supply split config");

        this.config = config;
        this.signKey = signKey;
    }

    private void setTimestampFunction(String timestampFunction) {
        String fn = UUID.randomUUID().toString().replace("-", "");
        try {
            this.engine.eval("function transform" + fn + "(ts) { return " + timestampFunction + "}");
        } catch (ScriptException e) {
            throw new RuntimeException(e);
        }
        this.timestampFunction = (String s) -> {
            Double out = (Double) engine.invokeFunction("transform" + fn, s);
            return out.longValue();
        };
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);

        this.topic = config.getTopic();
        this.jsonPath = JsonPath.compile(config.getSplitPath());
        this.headerPath = JsonPath.compile(config.getHeaderPath());
        this.tsField = config.getTimestampField();
        this.timestampSource = config.getTimestampSource();

        try {
            this.signature = Signature.getInstance("SHA1WithRSA");
            this.signature.initSign(signKey);
        } catch (NoSuchAlgorithmException | InvalidKeyException e) {
            log.error("Failed to initialise signing", e);
        }

        if(config.getTimestampFunction() != null & !config.getTimestampFunction().isEmpty()) {
            this.engine = new JavascriptEngineBuilder()
                    .script("")
                    .build();

            this.setTimestampFunction(config.getTimestampFunction());
        } else {
            this.engine = null;
            this.timestampFunction = (String s) -> {
                return Long.valueOf(s);
            };
        }

    }

    @Override
    public void flatMap(MessageToParse input, Collector<Message> collector) throws Exception {
        // sign the source content
        signature.update(input.getOriginalSource().getBytes(StandardCharsets.UTF_8));
        final sha1 sig = new sha1(signature.sign());

        DocumentContext documentContext = JsonPath.using(STRICT_PROVIDER_CONFIGURATION).parse(input.getOriginalSource());

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
                    .setOriginalSource(
                            SignedSourceKey.newBuilder()
                                    .setTopic(input.getTopic())
                                    .setPartition(input.getPartition())
                                    .setOffset(input.getOffset())
                                    .setSignature(sig)
                                    .build()
                    )
                    .setSource(this.topic)
                    .setTs(ts)
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
