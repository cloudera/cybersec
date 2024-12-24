/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.indexing.hive;

import static java.util.stream.Collectors.toMap;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.cloudera.cyber.scoring.Scores;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import javax.annotation.Nullable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.formats.json.JsonRowSerializationSchema;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableColumn;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;
import org.apache.hadoop.hive.conf.HiveConf;
import org.apache.hadoop.hive.metastore.HiveMetaStoreClient;
import org.apache.hadoop.hive.metastore.api.FieldSchema;
import org.apache.hive.streaming.HiveStreamingConnection;
import org.apache.hive.streaming.StreamingException;
import org.apache.hive.streaming.StrictJsonWriter;
import org.apache.thrift.TException;

/**
 * Stores events to a Hive table using Hive Streaming Data Ingest V2.
 *
 * <p>
 * https://cwiki.apache.org/confluence/display/Hive/Streaming+Data+Ingest+V2
 */
@Slf4j
@Data
@NoArgsConstructor
public class HiveStreamingMessageWriter {

    private static final String FIELDS_COLUMN_NAME = "fields";
    private static final String[] HIVE_SPECIAL_COLUMN_CHARACTERS = new String[] {".", ":"};
    private static final String[] HIVE_SPECIAL_COLUMN_REPLACEMENT_CHARACTERS = new String[] {"_", "_"};
    protected static final String DEFAULT_TIMESTAMP_FORMATS =
          "yyyy-MM-dd HH:mm:ss.SSSSSS,"
          + "yyyy-MM-dd'T'HH:mm:ss.SSS'Z',"
          + "yyyy-MM-dd'T'HH:mm:ss.SS'Z',"
          + "yyyy-MM-dd'T'HH:mm:ss.S'Z',"
          + "yyyy-MM-dd'T'HH:mm:ss'Z'";
    protected static final String HIVE_DATE_FORMAT = "yyyy-MM-dd HH:mm:ss.SSS";

    /**
     * Hive database containing the table to write events to.
     **/
    private String databaseName;
    /**
     * Hive table to write events to.
     **/
    private String tableName;
    /**
     * Hive streaming v2 connection batch size.
     **/
    private int batchSize;
    /**
     * Number of messages top send before ending the transaction.
     */
    private int messagesPerTransaction;
    /**
     * directory containing the hive configuration files - hive-site.xml.
     */
    private String hiveConfDir;
    /**
     * default java timestamp format to use when converting a string to a Hive timestamp.
     */
    private String possibleTimestampFormats;

    /**
     * connection to hive for streaming updates.
     **/
    private transient HiveStreamingConnection connection;
    /**
     * schema describing the columns in the hive table where the flink job will write events.
     **/
    private transient TableSchema tableSchema;
    /**
     * methods for mapping built in Message class fields into hive column values.
     **/
    private transient Map<String, Function<ScoredMessage, Object>> builtinColumnExtractor;
    /**
     * Type information for converting a single message to a json byte array.
     * The json is required by the StrictJsonWriter for the hive connection.
     **/
    private transient TypeInformation<Row> typeInfo;
    /**
     * methods for converting String extensions to the column values required by hive.
     **/
    private transient Map<DataType, Function<String, Object>> columnConversion;

    /**
     * schema for serializing a hive row to json for use with the StrictJsonWriter.
     */
    private transient JsonRowSerializationSchema jsonRowSerializationSchema;
    /**
     * date format for extracting the year month and date required by hive partitions.
     */
    private transient SimpleDateFormat dayFormat;
    /**
     * date format for extracting the hour required by hive partitions.
     */
    private transient SimpleDateFormat hourFormat;
    /**
     * date format for extracting the entire event timestamp to the format required by hive.
     */
    private transient SimpleDateFormat hiveTimestampFormat;
    /**
     * number of messages in the current transaction.  If set to zero, no transaction is open.
     */
    private transient int messagesInCurrentTransaction = 0;
    /**
     * names of the fields with type timestamp.
     * Keep track of them since they will need to be normalized and stored as a string.
     */
    private transient List<String> timestampFieldNames;
    /**
     * normalizer for non-built in timestamp fields.
     */
    private transient TimestampNormalizer timestampNormalizer;

    public HiveStreamingMessageWriter(ParameterTool params) {
        this.dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.hourFormat = new SimpleDateFormat("HH");
        this.hiveTimestampFormat = new SimpleDateFormat(HIVE_DATE_FORMAT);
        this.hiveConfDir = params.get("hive.confdir", "/etc/hive/conf");

        this.databaseName = params.get("hive.dbname", "cyber");
        this.tableName = params.get("hive.table", "events");
        this.batchSize = params.getInt("hive.batch.size", 1);
        this.messagesPerTransaction = params.getInt("hive.transaction.messages", 1000);
        this.possibleTimestampFormats = params.get("hive.timestamp.format", DEFAULT_TIMESTAMP_FORMATS);
    }

    public void connect() throws StreamingException, TException {
        HiveConf hiveConf = createHiveConf(hiveConfDir);
        StrictJsonWriter jsonWriter = StrictJsonWriter.newBuilder().build();

        connection = HiveStreamingConnection.newBuilder()
                                            .withDatabase(databaseName)
                                            .withTable(tableName)
                                            //.withStreamingOptimizations(true)
                                            .withTransactionBatchSize(batchSize)
                                            .withAgentInfo("flink-cyber")
                                            .withRecordWriter(jsonWriter)
                                            .withHiveConf(hiveConf)
                                            .connect();

        log.info("Hive Connection made {}", connection);

        tableSchema = getHiveTable(hiveConf, databaseName, tableName);

        builtinColumnExtractor = new HashMap<String, Function<ScoredMessage, Object>>() {
            {
                put("originalsource_topic", (m) -> m.getMessage().getOriginalSource().getTopic());
                put("originalsource_partition", (m) -> m.getMessage().getOriginalSource().getPartition());
                put("originalsource_offset", (m) -> m.getMessage().getOriginalSource().getOffset());
                put("originalsource_signature", (m) -> m.getMessage().getOriginalSource().getSignature());
                put("id", (m) -> m.getMessage().getId());
                put("ts", (m) -> formatMessageTimestamp(m.getMessage(), hiveTimestampFormat));
                put("message", (m) -> m.getMessage().getMessage());
                put("source", (m) -> m.getMessage().getSource());
                put("dt", (m) -> formatMessageTimestamp(m.getMessage(), dayFormat));
                put("hr", (m) -> formatMessageTimestamp(m.getMessage(), hourFormat));
                put("cyberscore", (m) -> m.getCyberScore().floatValue());
                put("cyberscore_details", HiveStreamingMessageWriter::convertCyberScoreDetailsToRow);
            }
        };

        columnConversion = new HashMap<DataType, Function<String, Object>>() {
            {
                put(DataTypes.DOUBLE(), Double::parseDouble);
                put(DataTypes.FLOAT(), Float::parseFloat);
                put(DataTypes.BIGINT(), Long::parseLong);
                put(DataTypes.INT(), Integer::parseInt);
                put(DataTypes.STRING(), (s) -> s);
                put(DataTypes.BOOLEAN(), Boolean::parseBoolean);
            }
        };

        typeInfo = new RowTypeInfo(tableSchema.getFieldTypes(), tableSchema.getFieldNames());

        timestampNormalizer = new TimestampNormalizer(possibleTimestampFormats, hiveTimestampFormat);

    }

    private static Row[] convertCyberScoreDetailsToRow(ScoredMessage scoredMessage) {
        Row[] rows = new Row[scoredMessage.getCyberScoresDetails().size()];
        int index = 0;
        for (Scores scoreDetail : scoredMessage.getCyberScoresDetails()) {
            Row scoreDetailRow = new Row(3);
            scoreDetailRow.setField(0, scoreDetail.getRuleId());
            scoreDetailRow.setField(1, scoreDetail.getScore().floatValue());
            scoreDetailRow.setField(2, scoreDetail.getReason());
            rows[index++] = scoreDetailRow;
        }
        return rows;
    }

    private static HiveConf createHiveConf(@Nullable String hiveConfDir) {
        log.info("Setting hive conf dir as {}", hiveConfDir);
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
        hadoopConf.addResource(hiveConfDir + "/hive-site.xml");
        hadoopConf.addResource(hiveConfDir + "/core-site.xml");
        try {
            HiveConf.setHiveSiteLocation(
                  hiveConfDir == null ? null : Paths.get(hiveConfDir, "hive-site.xml").toUri().toURL());
        } catch (MalformedURLException e) {
            log.error("Cannot load Hive Config", e);
        }
        return new HiveConf(hadoopConf, HiveConf.class);
    }

    private TableSchema getHiveTable(HiveConf hiveConf, String schema, String table) throws TException {
        HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
        List<FieldSchema> hiveFields = hiveMetaStoreClient.getSchema(schema, table);
        log.info("Read hive fields {}", hiveFields);
        // find all the timestamp fields that are not the built in timestamp field
        timestampFieldNames = hiveFields.stream()
                                        .filter(field -> !field.getName().equals("ts")
                                                         && field.getType().equalsIgnoreCase("timestamp"))
                                        .map(FieldSchema::getName).collect(Collectors.toList());
        return hiveFields.stream().reduce(TableSchema.builder(),
              (build, field) -> build.field(field.getName(), hiveTypeToDataType(field.getType())),
              (a, b) -> a
        ).build();
    }

    public static DataType hiveTypeToDataType(String type) {
        switch (type.toLowerCase()) {
            case "bigint":
                return DataTypes.BIGINT();
            case "int":
                return DataTypes.INT();
            case "map<string,string>":
                return DataTypes.MAP(DataTypes.STRING(), DataTypes.STRING());
            case "double":
                return DataTypes.DOUBLE();
            case "binary":
                return DataTypes.BYTES();
            case "boolean":
                return DataTypes.BOOLEAN();
            case "float":
                return DataTypes.FLOAT();
            case "array<struct<ruleid:string,score:float,reason:string>>":
                return DataTypes.ARRAY(
                      DataTypes.ROW(DataTypes.FIELD("ruleid", DataTypes.STRING()),
                            DataTypes.FIELD("score", DataTypes.FLOAT()),
                            DataTypes.FIELD("reason", DataTypes.STRING())));
            case "string":
            case "timestamp":
                return DataTypes.STRING();
            default:
                throw new IllegalArgumentException(
                      String.format("Data type '%s' in Hive schema is not supported.", type));
        }
    }

    byte[] serializeRow(Row row) {
        if (jsonRowSerializationSchema == null) {
            jsonRowSerializationSchema = new JsonRowSerializationSchema.Builder(typeInfo).build();
        }

        try {
            jsonRowSerializationSchema.open(null);
            return jsonRowSerializationSchema.serialize(row);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Unable to serialize row '%s'", row.toString()), e);
        }
    }

    private static String toHiveColumnName(String messageExtensionName) {
        return StringUtils.replaceEach(messageExtensionName, HIVE_SPECIAL_COLUMN_CHARACTERS,
              HIVE_SPECIAL_COLUMN_REPLACEMENT_CHARACTERS);
    }

    private static String formatMessageTimestamp(Message message, SimpleDateFormat dateFormat) {
        return formatEpochMillis(message.getTs(), dateFormat);
    }

    private static String formatEpochMillis(long epochMillis, SimpleDateFormat dateFormat) {
        return dateFormat.format(Date.from(Instant.ofEpochMilli(epochMillis)));
    }

    protected int addMessageToTransaction(ScoredMessage scoredMessage) throws Exception {

        beginTransaction();
        Message message = scoredMessage.getMessage();
        tableSchema.getFieldNames();
        log.debug("writing message {}", message);

        Row newRow = new Row(tableSchema.getTableColumns().size());
        int pos = 0;

        Map<String, String> extensions =
              message.getExtensions() != null ? message.getExtensions() : Collections.emptyMap();
        // create legal hive column names from the field name
        extensions = extensions.entrySet().stream().collect(
              toMap(e -> toHiveColumnName(e.getKey()), Map.Entry::getValue));

        for (TableColumn column : tableSchema.getTableColumns()) {
            String columnName = column.getName();
            Object columnValue = null;

            if (FIELDS_COLUMN_NAME.equals(columnName)) {
                // set the fields column equal to all the extensions that are not specifically pulled out into columns
                columnValue = extensions;
            } else {

                Function<ScoredMessage, Object> columnExtractor = builtinColumnExtractor.get(columnName);
                if (columnExtractor != null) {
                    columnValue = columnExtractor.apply(scoredMessage);
                } else {
                    String extensionValue = extensions.get(columnName);
                    if (extensionValue != null) {
                        try {
                            if (timestampFieldNames.contains(columnName)) {
                                columnValue = timestampNormalizer.apply(extensionValue);
                            } else {
                                columnValue = columnConversion.get(column.getType()).apply(extensionValue);
                            }
                            // stored as a separate column - remove from fields to avoid storing twice
                            extensions.remove(columnName);
                        } catch (Exception e) {
                            // leave value in field map
                            log.error(
                                  String.format("Could not convert value %s for column %s", extensionValue, columnName),
                                  e);
                        }
                    }
                }
            }

            newRow.setField(pos, columnValue);
            pos++;
        }
        byte[] rowJson = serializeRow(newRow);
        if (log.isDebugEnabled()) {
            log.debug("writing json {}", new String(rowJson));
        }
        connection.write(rowJson);
        messagesInCurrentTransaction++;

        return endTransaction();
    }

    protected void beginTransaction() throws Exception {
        if (messagesInCurrentTransaction == 0) {
            log.debug("Beginning hive transaction.");
            connection.beginTransaction();
            log.info("Hive transaction created.");
        }
    }

    protected int endTransaction() throws Exception {
        if (messagesInCurrentTransaction == messagesPerTransaction) {
            return commit();
        } else {
            log.debug("No transaction commit messagesInCurrentTransaction={}", messagesInCurrentTransaction);
            return 0;
        }
    }

    protected int commit() throws Exception {
        int messagesCommitted = messagesInCurrentTransaction;
        if (messagesInCurrentTransaction > 0) {
            log.debug("Committing {} messages to hive transaction", messagesInCurrentTransaction);
            connection.commitTransaction();
            messagesInCurrentTransaction = 0;
            log.info("Committed {} messages to hive", messagesCommitted);

        }
        return messagesCommitted;
    }

    protected void close() {
        if (connection != null) {
            log.info("Closing hive connection");
            // hive closes out current transaction
            connection.close();
            log.info("Hive connection closed");
        }
    }

}
