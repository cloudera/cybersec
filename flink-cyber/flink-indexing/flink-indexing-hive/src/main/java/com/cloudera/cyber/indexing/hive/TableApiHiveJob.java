package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.HiveColumnDto;
import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Streams;
import groovy.util.logging.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.FormatDescriptor;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.table.types.DataType;
import org.apache.flink.types.Row;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
public class TableApiHiveJob {

    private static final String HIVE_TABLES_INIT_FILE_PARAM = "hive.tables-init-file";
    private static final String HIVE_MAPPING_FILE_PARAM = "hive.mapping-file";

    private static final String BASE_COLUMN_MAPPING_JSON = "base-column-mapping.json";
    private static final String BASE_HIVE_TABLE_JSON = "base-table.json";
    private static final String KAFKA_TABLE = "KafkaTempView";


    private final List<MappingColumnDto> defaultMappingList;
    private final List<HiveColumnDto> defaultColumnList;

    private final DataStream<ScoredMessage> source;
    private final StreamExecutionEnvironment env;
    private final ParameterTool params;

    public TableApiHiveJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) throws IOException {
        this.params = params;
        this.env = env;
        this.source = source;
        defaultMappingList = Utils.readResourceFile(BASE_COLUMN_MAPPING_JSON, getClass(),
                new TypeReference<List<MappingColumnDto>>() {
                });
        defaultColumnList = Utils.readResourceFile(BASE_HIVE_TABLE_JSON, getClass(),
                new TypeReference<List<HiveColumnDto>>() {
                });
    }

    public void startJob() throws Exception {
        System.out.println("Creating Table env...");
        final StreamTableEnvironment tableEnv = getTableEnvironment();

        System.out.println("Configuring Table env...");
        configure(tableEnv);

        System.out.printf("Registering Hive catalog... %s%n", String.join(", ", tableEnv.listCatalogs()));
        registerHiveCatalog(tableEnv);

        System.out.println("Getting Hive tables config...");
        final Map<String, List<HiveColumnDto>> hiveTablesConfig = getHiveTablesConfig();

        System.out.println("Creating Hive tables...");
        final Set<String> tableList = new HashSet<>(Arrays.asList(tableEnv.listTables()));
        tableEnv.getConfig().setSqlDialect(SqlDialect.HIVE);

        hiveTablesConfig.forEach(
                (tableName, columnList) -> createHiveTableIfNotExists(tableEnv, tableList, tableName, columnList));
        tableEnv.getConfig().setSqlDialect(SqlDialect.DEFAULT);

        System.out.println("Getting topic mapping...");
        final Map<String, MappingDto> topicMapping = getTopicMapping();

        System.out.println("Creating Kafka table...");
        createKafkaTable(tableEnv);

        System.out.printf("Filling Insert statement list...%s%n", Arrays.toString(tableEnv.listTables()));
        final StreamStatementSet insertStatementSet = tableEnv.createStatementSet();

        // print the schema
        topicMapping.forEach((topic, mappingDto) -> {
            final String insertSql = buildInsertSql(topic, mappingDto);
            try {
                insertStatementSet.addInsertSql(insertSql);
                System.out.printf("Insert SQL added to the queue for the table: %s%nSQL: %s%n", mappingDto.getTableName(), insertSql);
            } catch (Exception e) {
                System.err.printf("Error adding insert to the statement set: %s%n", insertSql);
                throw e;
            }
        });
        System.out.println("Executing Insert statement list...");
        insertStatementSet.execute();

        System.out.println("TableApiHiveJob is done!");
    }

    private void createHiveTableIfNotExists(StreamTableEnvironment tableEnv, Set<String> tableList, String hiveTableName, List<HiveColumnDto> columnList) {
        if (tableList.contains(hiveTableName)) {
            System.out.printf("Hive table [%s] already exists. Skipping its creation.%n", hiveTableName);
        } else {
            System.out.printf("Creating Hive table %s...%n", hiveTableName);
            final String tableConnector = getTableConnector();
            final Schema schema = buildHiveTableSchema(columnList);
            try {
                final TableDescriptor tableDescriptor = buildTableDescriptor(tableConnector, schema);
                System.out.println("TableDescriptor: " + tableDescriptor.toString());
                tableEnv.createTable(hiveTableName, tableDescriptor);
            } catch (Exception e) {
                System.err.printf("Error creating table with connector: [%s] and schema: %s%n", tableConnector, schema);
                throw e;
            }
            System.out.printf("Hive table created: %s%n", hiveTableName);
        }
    }

    private static TableDescriptor buildTableDescriptor(String tableConnector, Schema schema) {
        return TableDescriptor
                .forConnector(tableConnector)
                .schema(schema)
                .partitionedBy("dt", "hr")
                .format(FormatDescriptor.forFormat("parquet").build())
                .option("partition.time-extractor.timestamp-pattern", "$dt $hr:00:00")
                .option("sink.partition-commit.trigger", "process-time")
//                .option("sink.partition-commit.delay", "1 h")
                .option("sink.partition-commit.policy.kind", "metastore,success-file")
                .build();
    }

    private Schema buildHiveTableSchema(List<HiveColumnDto> columnList) {
        final List<Column> flinkColumnList = columnList.stream()
                .map(col -> Column.physical(col.getName(), getFlinkType(col.getType(), col.getNullable())))
                .collect(Collectors.toList());
        System.out.println("flinkColumnList: " + flinkColumnList.stream().map(Column::toString).collect(Collectors.joining(", ")));
        return Schema.newBuilder()
                .fromResolvedSchema(ResolvedSchema.of(flinkColumnList))
                .build();
    }

    private static DataType getFlinkType(String colType) {
        return getFlinkType(colType, true);
    }

    /**
     * Creates Flink Data Type from the config column type.
     *
     * @param colType  config column type.
     *                 Possible config column type values are:
     *                 string, timestamp, date, int, bigint, float, double, boolean, bytes, null,
     *                 array<type>, map<key,value>, struct<field:type, field2:type2>
     * @param nullable whether column is nullable or not
     * @return Flink DataType that describes provided column type
     */
    private static DataType getFlinkType(String colType, Boolean nullable) {
        if (colType == null) {
            throw new IllegalArgumentException("Column type cannot be null");
        }
        final String type = colType.toLowerCase().trim();
        DataType result;
        if (type.equals("string")) {
            result = DataTypes.STRING();
        } else if (type.equals("timestamp")) {
            result = DataTypes.TIMESTAMP(9);
        } else if (type.equals("date")) {
            result = DataTypes.DATE();
        } else if (type.equals("int")) {
            result = DataTypes.INT();
        } else if (type.equals("bigint")) {
            result = DataTypes.BIGINT();
        } else if (type.equals("float")) {
            result = DataTypes.FLOAT();
        } else if (type.equals("double")) {
            result = DataTypes.DOUBLE();
        } else if (type.equals("boolean")) {
            result = DataTypes.BOOLEAN();
        } else if (type.equals("bytes")) {
            result = DataTypes.BYTES();
        } else if (type.equals("null")) {
            result = DataTypes.NULL();
        } else if (type.startsWith("array")) {
            result = parseArrayType(type);
        } else if (type.startsWith("map")) {
            result = parseMapType(type);
        } else if (type.startsWith("struct")) {
            result = parseStructType(type);
        } else {
            throw new IllegalArgumentException("Unknown column type: " + type);
        }
        return Optional.ofNullable(nullable).orElse(true) ? result.nullable() : result.notNull();
    }

    /**
     * Parses Array type from the config column type.
     *
     * @param type config column type. Supported format is: array<type>
     * @return Flink DataType that describes provided array type
     */
    private static DataType parseArrayType(String type) {
        final String body = getInnerBody(type, "Array");
        return DataTypes.ARRAY(getFlinkType(body));
    }

    /**
     * Parses Map type from the config column type.
     *
     * @param type config column type. Supported format is: map<key,value>
     * @return Flink DataType that describes provided map type
     */
    private static DataType parseMapType(String type) {
        final String body = getInnerBody(type, "Map");
        if (!body.contains(",")) {
            throw new IllegalArgumentException("Unknown column type for Map: " + type);
        }
        final List<String> split = splitTypes(body, ',');
        return DataTypes.MAP(getFlinkType(split.get(0)), getFlinkType(split.get(1)));
    }

    /**
     * Parses Struct type from the config column type.
     *
     * @param type config column type. Supported format is: struct<name:type, name2:type2>. The name can contain only alphanumeric characters and underscores.
     * @return Flink DataType that describes provided struct type
     */
    private static DataType parseStructType(String type) {
        final String body = getInnerBody(type, "Struct");
        if (!body.contains(":")) {
            throw new IllegalArgumentException("Unknown column type for Struct: " + type);
        }
        final List<String> fields = splitTypes(body, ',');
        final List<DataTypes.Field> fieldList = new ArrayList<>();
        for (String field : fields) {
            final List<String> fieldSplit = splitTypes(field, ':');
            if (fieldSplit.size() < 2) {
                throw new IllegalArgumentException("Unknown column type for Struct: " + type);
            }
            final String name = fieldSplit.get(0).replaceAll("[^a-zA-Z0-9_]", "");
            final DataType value = getFlinkType(fieldSplit.get(1));
            fieldList.add(DataTypes.FIELD(name, value));
        }
        return DataTypes.ROW(fieldList.toArray(new DataTypes.Field[0]));
    }

    private static String getInnerBody(String type, String typeName) {
        final int start = type.indexOf("<");
        final int stop = type.lastIndexOf(">");
        if (start < 0 || stop < 0) {
            throw new IllegalArgumentException("Unknown column type for " + typeName + " : " + type);
        }
        return type.substring(start + 1, stop);
    }

    private static List<String> splitTypes(String body, char delimiter) {
        List<String> types = new ArrayList<>();
        int start = 0;
        int end = 0;
        int count = 0;
        while (end < body.length()) {
            if (body.charAt(end) == '<') {
                count++;
            } else if (body.charAt(end) == '>') {
                count--;
            } else if (body.charAt(end) == delimiter && count == 0) {
                types.add(body.substring(start, end));
                start = end + 1;
            }
            end++;
        }
        types.add(body.substring(start, end));
        return types;
    }

    private String getTableConnector() {
        return "hive";
    }

    private StreamTableEnvironment getTableEnvironment() {
        return StreamTableEnvironment.create(env);
    }

    private void createKafkaTable(StreamTableEnvironment tableEnv) {
        final SingleOutputStreamOperator<Row> newSource = source.map(ScoredMessage::toRow,
                ScoredMessage.FLINK_TYPE_INFO);
        tableEnv.createTemporaryView(KAFKA_TABLE, newSource);
        tableEnv.executeSql("DESCRIBE " + KAFKA_TABLE).print();
    }

    private void registerHiveCatalog(StreamTableEnvironment tableEnv) {
        String name = "hive";
        String defaultDatabase = params.get("hive.dbname", "cyber");
        String hiveConfDir = params.get("hive.confdir", "/etc/hive/conf");

        HiveCatalog hive = new HiveCatalog(name, defaultDatabase, hiveConfDir);
        tableEnv.registerCatalog(name, hive);
        tableEnv.useCatalog(name);
    }

    private Map<String, MappingDto> getTopicMapping() throws IOException {
        TypeReference<HashMap<String, MappingDto>> typeRef
                = new TypeReference<HashMap<String, MappingDto>>() {
        };
        final HashMap<String, MappingDto> columnMappingMap = Utils.readFile(params.getRequired(HIVE_MAPPING_FILE_PARAM), typeRef);

        //adding the default column mappings to each topic
        columnMappingMap.values().forEach(mapping -> {
            final List<MappingColumnDto> customMappings = Optional.ofNullable(mapping.getColumnMapping())
                    .orElse(Collections.emptyList());
            final Collection<MappingColumnDto> columnMapping = Streams.concat(
                            customMappings.stream(),
                            defaultMappingList.stream())
                    .collect(Collectors.toMap(MappingColumnDto::getName, Function.identity(), (f, s) -> f)).values();

            mapping.setColumnMapping(new ArrayList<>(columnMapping));
        });
        return columnMappingMap;
    }

    private Map<String, List<HiveColumnDto>> getHiveTablesConfig() throws IOException {
        TypeReference<HashMap<String, List<HiveColumnDto>>> typeRef
                = new TypeReference<HashMap<String, List<HiveColumnDto>>>() {
        };
        final String filePath = params.get(HIVE_TABLES_INIT_FILE_PARAM);
        if (filePath == null) {
            return Collections.emptyMap();
        }
        final HashMap<String, List<HiveColumnDto>> columnMap = Utils.readFile(filePath, typeRef);
        final List<HiveColumnDto> partitionColumns = Arrays.asList(HiveColumnDto.builder()
                .name("dt")
                .type("string")
                .build(), HiveColumnDto.builder()
                .name("hr")
                .type("string")
                .build());
        //adding the default columns to each table
        columnMap.forEach((tableName, columnList) -> {
            final List<HiveColumnDto> customColumns = Optional.ofNullable(columnList)
                    .orElse(Collections.emptyList());
            final Map<String, HiveColumnDto> combinedColumnMap = Streams.concat(customColumns.stream(), defaultColumnList.stream(), partitionColumns.stream())
                    .collect(Collectors.toMap(HiveColumnDto::getName, Function.identity(), (f, s) -> f, LinkedHashMap::new));
            //partition columns should be placed last
            partitionColumns.forEach(col -> combinedColumnMap.put(col.getName(), col));

            columnMap.put(tableName, new ArrayList<>(combinedColumnMap.values()));
        });
        return columnMap;
    }

    private void configure(StreamTableEnvironment tableEnv) {
        final HashMap<String, String> conf = new HashMap<>();
        conf.put("pipeline.name", params.get("flink.job.name", "Indexing - Hive TableApi"));
        tableEnv.getConfig().addConfiguration(Configuration.fromMap(conf));
        tableEnv.createTemporarySystemFunction("filterMap", FilterMapFunction.class);
    }

    private String buildInsertSql(String topic, MappingDto mappingDto) {
        return String.join("\n", "insert into " + mappingDto.getTableName() + "(" + getHiveInsertColumns(mappingDto) + ")",
                " SELECT " + getKafkaFromColumns(mappingDto),
                " from " + KAFKA_TABLE,
                String.format(" where `message`.`originalSource`.`topic`='%s'", topic));
    }

    private String getKafkaFromColumns(MappingDto mappingDto) {
        return mappingDto.getColumnMapping().stream()
                .map(mappingColumnDto -> {
                    final String kafkaName = mappingColumnDto.getKafkaName();
                    final String path = mappingColumnDto.getPath();

                    String fullPath;
                    if (path.startsWith("..")) {
                        fullPath = path.substring(2);
                    } else {
                        fullPath = String.format("message.%s", path);
                    }
                    if (StringUtils.hasText(fullPath)) {
                        fullPath = Arrays.stream(fullPath.split("\\."))
                                .collect(Collectors.joining(".", "", ""));
                    }

                    fullPath = fullPath + kafkaName;

                    final String transformation = mappingColumnDto.getTransformation();
                    return StringUtils.hasText(transformation)
                            ? String.format(transformation, "(" + fullPath + ")", mappingDto.getIgnoreFields().stream()
                            .collect(Collectors.joining("','", "'", "'")))
                            : fullPath;
                })
                .collect(Collectors.joining(", ", " ", " "));
    }

    private String getHiveInsertColumns(MappingDto mappingDto) {
        return mappingDto.getColumnMapping().stream()
                .map(MappingColumnDto::getName)
                .collect(Collectors.joining(", ", " ", " "));
    }

}
