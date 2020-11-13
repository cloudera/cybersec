package com.cloudera.cyber.indexing.hive;

import lombok.extern.slf4j.Slf4j;
import org.apache.flink.table.descriptors.ConnectorDescriptor;

import java.util.HashMap;
import java.util.Map;

@Slf4j
public class HiveStreamingConnector extends ConnectorDescriptor {

    private String hiveSchema;
    private String hiveTable;

    public HiveStreamingConnector(String hiveSchema, String hiveTable) {
        super("hive-cyber", 1, false);
        this.hiveSchema = hiveSchema;
        this.hiveTable = hiveTable;
    }

    @Override
    protected Map<String, String> toConnectorProperties() {
        Map<String, String> properties = new HashMap<>();
        properties.put("hive.schema", this.hiveSchema);
        properties.put("hive.table", this.hiveTable);

        log.info("Hive Streaming Properties: " + properties.toString());
        return properties;
    }
}
