package com.cloudera.cyber.test.generator;

import java.nio.charset.StandardCharsets;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.configuration.Configuration;

public class AvroMapFunction extends RichMapFunction<Tuple2<String, String>, Tuple2<String, byte[]>> {

    private final String schemaString;
    private transient Schema schema;

    AvroMapFunction(String schemaString) {
        this.schemaString = schemaString;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        this.schema =  new Parser().parse(schemaString);

    }

    @Override
    public Tuple2<String, byte[]> map(Tuple2<String, String> tuple2) {
        return new Tuple2<>(tuple2.f0, Utils.jsonDecodeToAvroByteArray(tuple2.f1, schema));
    }
}
