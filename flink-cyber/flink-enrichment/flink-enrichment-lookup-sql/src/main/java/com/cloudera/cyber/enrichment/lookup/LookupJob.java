package com.cloudera.cyber.enrichment.lookup;

import com.cloudera.cyber.EnrichmentEntry;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.lookup.config.EnrichmentConfig;
import com.cloudera.cyber.flink.CyberJob;
import org.apache.flink.api.common.functions.FlatMapFunction;
import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.tuple.Tuple3;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.TimeCharacteristic;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.streaming.api.functions.timestamps.AscendingTimestampExtractor;
import org.apache.flink.streaming.api.functions.timestamps.BoundedOutOfOrdernessTimestampExtractor;
import org.apache.flink.streaming.api.windowing.time.Time;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.java.StreamTableEnvironment;
import org.apache.flink.table.functions.TemporalTableFunction;
import org.apache.flink.types.Row;
import org.apache.flink.util.Collector;
import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.type.TypeReference;

import java.io.ByteArrayInputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

/**
 * Create an input stream of lookup sources which are materialize in a local dynamic table
 * <p>
 * The dynamic table is used for lookups to augment a passing Message stream
 */
public abstract class LookupJob implements CyberJob, MessageProcessingJob {

    @Override
    public StreamExecutionEnvironment createPipeline(ParameterTool params) throws Exception {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        env.setStreamTimeCharacteristic(TimeCharacteristic.EventTime);

        byte[] configJson = Files.readAllBytes(Paths.get("config.json"));
        List<EnrichmentConfig> configs = new ObjectMapper().readValue(
                configJson,
                new TypeReference<List<EnrichmentConfig>>() {
                });

        StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env, EnvironmentSettings
                .newInstance()
                .useBlinkPlanner()
                .inStreamingMode()
                .build());

        DataStream<Enrichment> enrichments = createEnrichmentSource(env, params)
                .assignTimestampsAndWatermarks(new AscendingTimestampExtractor<EnrichmentEntry>() {
                    @Override
                    public long extractAscendingTimestamp(EnrichmentEntry enrichmentEntry) {
                        return enrichmentEntry.getTs();
                    }
                })
                .map(r -> Enrichment.builder()
                        .ts(r.getTs())
                        .pri(r.getType() + ":" + r.getKey())
                        .entries(r.getEntries())
                        .build());

        Table tEnrichments = tableEnv.fromDataStream(enrichments, "ts.rowtime, pri, entries");

        tableEnv.createTemporaryView("enrichments", tEnrichments);

        TemporalTableFunction fEnrichments = tEnrichments.createTemporalTableFunction("ts", "pri");
        tableEnv.registerFunction("enrichments", fEnrichments);

        DataStream<Message> messages = createSource(env, params)
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<Message>(Time.milliseconds(100)) {
                    @Override
                    public long extractTimestamp(Message message) {
                        return message.getTs();
                    }
                });
        //Table tMessages = tableEnv.fromDataStream(messages, "id, ts.rowtime, extensions");
        Table tRawMessages = tableEnv.fromDataStream(messages.map(new MapFunction<Message, Tuple3<String, Long, byte[]>>() {
            @Override
            public Tuple3<String, Long, byte[]> map(Message message) throws Exception {
                return Tuple3.of(message.getId(), message.getTs(), message.toByteBuffer().array());
            }
        }), "id, ts, b");
        tableEnv.createTemporaryView("messages", tRawMessages);

        DataStream<Enrichable> enrichables = messages.flatMap(new ExtractEnrichables(configs))
                .assignTimestampsAndWatermarks(new BoundedOutOfOrdernessTimestampExtractor<Enrichable>(Time.milliseconds(100)) {
                    @Override
                    public long extractTimestamp(Enrichable enrichable) {
                        return enrichable.getTs();
                    }
                });

        Table tEnrichable = tableEnv.fromDataStream(enrichables, "ts.rowtime, id, lookupKey, field, enrichmentType");
        tableEnv.createTemporaryView("toEnrich", tEnrichable);

        System.out.println("Messages");
        tRawMessages.printSchema();

        System.out.println("Enrichables");
        tEnrichable.printSchema();
        tableEnv.toAppendStream(tEnrichable, Row.class).print();
        System.out.println("Enrichments");
        tEnrichments.printSchema();
        tableEnv.toAppendStream(tEnrichments, Row.class).print();

        // do the joins
        Table out = tableEnv.sqlQuery("SELECT toEnrich.id, toEnrich.field, toEnrich.enrichmentType, e.entries " +
                "FROM toEnrich, " +
                "LATERAL TABLE (enrichments(toEnrich.ts)) as e " +
                "WHERE toEnrich.lookupKey = e.pri");

        tableEnv.createTemporaryView("output", out);

        TemporalTableFunction fOutput = tEnrichments.createTemporalTableFunction("ts", "pri");
        tableEnv.registerFunction("enrichments", fEnrichments);


        tableEnv.registerFunction("aggregateEnrichments", new EnrichmentAggregator());

        final String messageFields = "messages.b";
        Table joined = tableEnv.sqlQuery("SELECT " + messageFields +
                ", aggregateEnrichments(output.field, output.enrichmentType, output.entries) AS enrichments " +
                "FROM messages LEFT JOIN output ON output.id = messages.id " +
                "GROUP BY messages.b");
        joined.printSchema();

        tableEnv.toAppendStream(out, Row.class).print();

        DataStream<Message> results = retractable(tableEnv, joined);
        //DataStream<Message> results = appendable(tableEnv, joined);

        // figure out a way to materialise the retractable stream to output single view of the message based on a watermark

        results.print();

        writeResults(env, params, results);
        return env;
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
