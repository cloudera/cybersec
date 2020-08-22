package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.flink.CyberJob;
import com.google.common.collect.Streams;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.functions.TemporalTableFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.ArrayUtils;
import org.apache.flink.util.Collector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Create an input stream of lookup sources which are materialize in a local dynamic table
 * <p>
 * The dynamic table is used for lookups to augment a passing Message stream
 */
@Slf4j
public abstract class LookupJobWide implements CyberJob, MessageProcessingJob {

    @Override
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build());

        byte[] configJson = Files.readAllBytes(Paths.get("config.json"));
        List<EnrichmentConfig> configs = new ObjectMapper().readValue(
                configJson,
                new TypeReference<List<EnrichmentConfig>>() {
                });

        createEnrichments(env, tableEnv, params);
        DataStream<Message> messages = createSource(env, params)
//                .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<Message>() {
//            @Override
//            public long extractAscendingTimestamp(Message message) {
//                return message.getTs();
//            }
//        })
                ;

        List<Tuple2<String, String>> fieldToType = configs.stream()
                .flatMap(c -> c.getFields().stream().map(f -> Tuple2.of(f.getName(), f.getEnrichmentType())))
                .collect(Collectors.toList());

        String[] fieldNames = ArrayUtils.concat(
                new String[]{"b", "ts"},
                fieldToType.stream().map(f -> f.f0 + "_" + f.f1).toArray(s -> new String[s])
        );
        TypeInformation[] fieldTypes = Streams.concat(
                Stream.of(TypeInformation.of(byte[].class), Types.LONG),
                fieldToType.stream().map(f -> Types.STRING)
        ).toArray(TypeInformation[]::new);

        DataStream<Row> toEnrich = messages
                .map(m -> {
                    Stream<String> results = fieldToType.stream().map(f ->
                            (m.getExtensions().containsKey(f.f0) ?
                                    m.getExtensions().get(f.f0).toString() :
                                    null)
                    );
                    return Row.of(Streams.concat(Stream.of(m.toByteBuffer().array(), m.getTs()), results).toArray());
                })
                .returns(Types.ROW_NAMED(fieldNames, fieldTypes))
                .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<Row>() {
                    @Override
                    public long extractAscendingTimestamp(Row row) {
                        return (long) row.getField(1);
                    }
                });

        String fExp = "b, ts.rowtime, " +
                fieldToType.stream()
                        .map(f -> String.format("%s_%s",f.f0,f.f1))
                        .collect(Collectors.joining(","));

        Table tToEnrich = tableEnv.fromDataStream(toEnrich, fExp);

        tableEnv.createTemporaryView("toEnrich", tToEnrich);

        tToEnrich.printSchema();;
        tableEnv.toAppendStream(tToEnrich, Row.class).print();

        String sql = "SELECT e.b, \n" +
                fieldToType.stream()
                        .map(f -> f.f0 + "_" + f.f1)
                        .map(f -> String.format("`e_%s`.entries as %s", f, f))
                        .collect(Collectors.joining(",\n"))
                + "\nFROM toEnrich e \n" +
                fieldToType.stream()
                        .map(f -> f.f0 + "_" + f.f1)
                        .map(f -> String.format("LEFT JOIN enrichments as `e_%s` ON `e_%s`.pri = e.`%s`", f, f, f))
                        .collect(Collectors.joining(" \n"));
        log.info(sql);

        DataStream<Message> results = tableEnv.toAppendStream(tableEnv.sqlQuery(sql), Row.class).map(
                r -> {
                    Message old = Message.getDecoder().decode((byte[]) r.getField(0));
                    HashMap<String, String> fields = new HashMap<String, String>();
                    for (int i = 1; i < r.getArity(); i++) {
                        Map<String, String> entries = (Map<String, String>) r.getField(i);
                        final int finalI = i;
                        Map<String, String> output = entries.entrySet().stream().collect(
                                Collectors.toMap(k -> fieldNames[finalI] + "_" + k.getKey(),
                                        v -> v.getValue())
                        );
                        fields.putAll(output);
                    }
                    return MessageUtils.addFields(old, fields);

                }
        );
        writeResults(env, params, results);

        return env;
    }

    private byte[] encode(Message message) throws IOException {
        return message.toByteBuffer().array();
    }

    private void createEnrichments(StreamExecutionEnvironment env, StreamTableEnvironment tableEnv, ParameterTool params) {
        DataStream<Enrichment> enrichments = createEnrichmentSource(env, params)
//                .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<EnrichmentEntry>() {
//                    @Override
//                    public long extractAscendingTimestamp(EnrichmentEntry enrichmentEntry) {
//                        return enrichmentEntry.getTs();
//                    }
//                })
                .map(r -> Enrichment.builder()
                        .ts(r.getTs())
                        .pri(r.getType() + ":" + r.getKey())
                        .entries(r.getEntries())
                        .build());

        Table tEnrichments = tableEnv.fromDataStream(enrichments, "ts.rowtime, pri, entries");

        tableEnv.createTemporaryView("enrichments", tEnrichments);

        TemporalTableFunction fEnrichments = tEnrichments.createTemporalTableFunction("ts", "pri");
        tableEnv.registerFunction("enrichments", fEnrichments);


    }

    protected DataStream<Message> appendable(StreamTableEnvironment tableEnv, Table joined) {
        return tableEnv.toAppendStream(joined, Row.class)
                .flatMap(new FlatMapFunction<Row, Message>() {
                    @Override
                    public void flatMap(Row r, Collector<Message> c) throws Exception {
                        Message m = MessageUtils.addFields(
                                Message.getDecoder().decode(new ByteArrayInputStream((byte[]) r.getField(0))),
                                (Map<String, String>) r.getField(1)
                        );
                        c.collect(m);

                    }
                });

    }

    protected DataStream<Message> retractable(StreamTableEnvironment tableEnv, Table joined) {
        return tableEnv.toRetractStream(joined, Row.class)
                .flatMap(new FlatMapFunction<Tuple2<Boolean, Row>, Message>() {
                    @Override
                    public void flatMap(Tuple2<Boolean, Row> r, Collector<Message> c) throws Exception {
                        if (r.f0) {
                            Message m = MessageUtils.addFields(
                                    Message.getDecoder().decode(new ByteArrayInputStream((byte[]) r.f1.getField(0))),
                                    (Map<String, String>) r.f1.getField(1)
                            );
                            c.collect(m);
                        }
                    }
                });
    }

    ;

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params, DataStream<Message> results);

    public abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);

    protected abstract DataStream<EnrichmentEntry> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params);

}
