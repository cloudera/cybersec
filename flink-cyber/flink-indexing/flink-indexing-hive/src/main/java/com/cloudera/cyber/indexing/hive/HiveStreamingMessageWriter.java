package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.Message;
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

import javax.annotation.Nullable;
import java.net.MalformedURLException;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Stores events to a Hive table using Hive Streaming Data Ingest V2.
 *
 * https://cwiki.apache.org/confluence/display/Hive/Streaming+Data+Ingest+V2
 */
@Slf4j
@Data
@NoArgsConstructor
public class HiveStreamingMessageWriter  {

    private static final String FIELDS_COLUMN_NAME = "fields";
    private static final String[] HIVE_SPECIAL_COLUMN_CHARACTERS = new String[] { ".", ":"};
    private static final String[] HIVE_SPECIAL_COLUMN_REPLACEMENT_CHARACTERS = new String[] {"_", "_"};

    /** Hive database containing the table to write events to **/
    private String databaseName;
    /** Hive table to write events to **/
    private String tableName;
    /** Hive streaming v2 connection batch size **/
    private int batchSize;
    /** Number of messages top send before ending the transaction */
    private int messagesPerTransaction;
    /** directory containing the hive configuration files - hive-site.xml */
    private String hiveConfDir;

    /** connection to hive for streaming updates **/
    private transient HiveStreamingConnection connection;
    /** schema describing the columns in the hive table where the flink job will write events **/
    private transient TableSchema tableSchema;
    /** methods for mapping built in Message class fields into hive column values **/
    private transient Map<String, Function<Message, Object>> builtinColumnExtractor;
    /** Type information for converting a single message to a json byte array.  The json is required by the StrictJsonWriter for the hive connection. **/
    private transient TypeInformation<Row> typeInfo;
    /** methods for converting String extensions to the column values required by hive. **/
    private transient Map<DataType, Function<String, Object>> columnConversion;

    /** schema for serializing a hive row to json for use with the StrictJsonWriter */
    private transient JsonRowSerializationSchema jsonRowSerializationSchema;
    /** date format for extracting the year month and date required by hive partitions */
    private transient SimpleDateFormat dayFormat;
    /** date format for extracting the hour required by hive partitions */
    private transient SimpleDateFormat hourFormat;
    /** date format for extracting the entire event timestamp to the format required by hive */
    private transient SimpleDateFormat hiveTimestampFormat;
    /** number of messages in the current transaction.  If set to zero, no transaction is open */
    private transient int messagesInCurrentTransaction = 0;

    public HiveStreamingMessageWriter(ParameterTool params) {
        this.dayFormat = new SimpleDateFormat("yyyy-MM-dd");
        this.hourFormat = new SimpleDateFormat("HH");
        this.hiveTimestampFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS");
        this.hiveConfDir = params.get("hive.confdir","/etc/hive/conf");

        this.databaseName = params.get("hive.dbname", "cyber");
        this.tableName = params.get("hive.table", "events");
        this.batchSize = params.getInt("hive.batch.size", 1);
        this.messagesPerTransaction = params.getInt("hive.transaction.messages", 1000);
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

        builtinColumnExtractor = new HashMap<String, Function<Message, Object>>() {{
            put("originalsource_topic", (m) -> m.getOriginalSource().getTopic());
            put("originalsource_partition", (m) -> m.getOriginalSource().getPartition());
            put("originalsource_offset", (m) -> m.getOriginalSource().getOffset());
            put("originalsource_signature", (m) -> m.getOriginalSource().getSignature());
            put("id", Message::getId);
            put("ts", (m) -> formatMessageTimestamp(m, hiveTimestampFormat));
            put("message", Message::getMessage);
            put("source", Message::getSource);
            put("dt", (m) -> formatMessageTimestamp(m, dayFormat));
            put("hr", (m) -> formatMessageTimestamp(m, hourFormat));
        }};

        columnConversion = new HashMap<DataType, Function<String, Object>>() {{
            put(DataTypes.DOUBLE(), Double::parseDouble);
            put(DataTypes.BIGINT(), Long::parseLong);
            put(DataTypes.STRING(), (s) -> s);
            put(DataTypes.BOOLEAN(), Boolean::parseBoolean);
        }};

        typeInfo = new RowTypeInfo(tableSchema.getFieldTypes(), tableSchema.getFieldNames());
    }

    private static HiveConf createHiveConf(@Nullable String hiveConfDir) {
        log.info("Setting hive conf dir as {}", hiveConfDir);
        org.apache.hadoop.conf.Configuration hadoopConf = new org.apache.hadoop.conf.Configuration();
        hadoopConf.addResource(hiveConfDir +"/hive-site.xml");
        hadoopConf.addResource(hiveConfDir + "/core-site.xml");
        try {
            HiveConf.setHiveSiteLocation(hiveConfDir == null ? null : Paths.get(hiveConfDir, "hive-site.xml").toUri().toURL());
        } catch (MalformedURLException e) {
            log.error("Cannot load Hive Config", e);
        }
        return new HiveConf(hadoopConf, HiveConf.class);
    }

    private TableSchema getHiveTable( HiveConf hiveConf, String schema, String table) throws TException {
        HiveMetaStoreClient hiveMetaStoreClient = new HiveMetaStoreClient(hiveConf);
        List<FieldSchema> hiveFields = hiveMetaStoreClient.getSchema(schema, table);
        log.info("Read hive fields {}", hiveFields);
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
            default:
                return DataTypes.STRING();
        }
    }

    byte[] serializeRow(Row row) {
        if (jsonRowSerializationSchema == null)
            jsonRowSerializationSchema = new JsonRowSerializationSchema.Builder(typeInfo).build();
        return jsonRowSerializationSchema.serialize(row);
    }

    private static String toHiveColumnName(String messageExtensionName) {
        return StringUtils.replaceEach(messageExtensionName, HIVE_SPECIAL_COLUMN_CHARACTERS, HIVE_SPECIAL_COLUMN_REPLACEMENT_CHARACTERS);
    }

    private static String formatMessageTimestamp(Message message, SimpleDateFormat dateFormat) {
        return dateFormat.format(Date.from(Instant.ofEpochMilli(message.getTs())));
    }

    protected int addMessageToTransaction(Message message) throws Exception {

        beginTransaction();
        tableSchema.getFieldNames();
        log.debug("writing message {}", message);

        Row newRow = new Row(tableSchema.getTableColumns().size());
        int pos = 0;

        Map<String, String> extensions = message.getExtensions() != null ? message.getExtensions() : Collections.emptyMap();
        // create legal hive column names from the field name
        extensions = extensions.entrySet().stream().collect(
                toMap(e -> toHiveColumnName(e.getKey()), Map.Entry::getValue));

        for(TableColumn column : tableSchema.getTableColumns()) {
            String columnName = column.getName();
            Object columnValue = null;

            if (FIELDS_COLUMN_NAME.equals(columnName)) {
                // set the fields column equal to all the extensions that are not specifically pulled out into columns
                columnValue = extensions;
            } else {

                Function<Message, Object> columnExtractor = builtinColumnExtractor.get(columnName);
                if (columnExtractor != null) {
                    columnValue = columnExtractor.apply(message);
                } else {
                    String extensionValue = extensions.get(columnName);
                    if (extensionValue != null) {
                        try {
                            columnValue = columnConversion.get(column.getType()).apply(extensionValue);
                            // stored as a separate column - remove from fields to avoid storing twice
                            extensions.remove(columnName);
                        } catch (Exception e) {
                            // leave value in field map
                           log.error(String.format("Could not convert value %s for column %s", extensionValue, columnName), e);
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
