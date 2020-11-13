package com.cloudera.cyber.indexing.hive;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.descriptors.DescriptorProperties;
import org.apache.flink.table.factories.StreamTableSinkFactory;
import org.apache.flink.table.sinks.StreamTableSink;
import org.apache.flink.table.utils.TableSchemaUtils;
import org.apache.flink.types.Row;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
public class HiveStreamingTableSinkFactory implements StreamTableSinkFactory<Row> {

    @Override
    public StreamTableSink<Row> createStreamTableSink(Map<String, String> properties) {
        DescriptorProperties params = new DescriptorProperties();
        params.putProperties(properties);

        TableSchema schema = TableSchemaUtils.getPhysicalSchema(params.getTableSchema("schema"));
        return new HiveStreamingTableSink(schema, params.getString("hive.schema"), params.getString("hive.table"));
    }

    @Override
    public Map<String, String> requiredContext() {
        Map<String, String> context = new HashMap();
        context.put("connector.type", "hive-cyber");
        context.put("connector.property-version", "1");
        //context.put("format.type", "hive-cyber");
        //context.put("format.property-version", "1");
        context.put("update-mode", "append");
        return context;

    }

    @Override
    public List<String> supportedProperties() {
        List<String> properties = new ArrayList();
        properties.add("format.fields.#.type");
        properties.add("format.fields.#.data-type");
        properties.add("format.fields.#.name");
        properties.add("format.derive-schema");
        properties.add("format.field-delimiter");
        properties.add("schema.#.type");
        properties.add("schema.#.data-type");
        properties.add("schema.#.name");
        properties.add("schema.watermark.*");

        properties.add("hive.schema");
        properties.add("hive.table");
        return properties;
    }
}
