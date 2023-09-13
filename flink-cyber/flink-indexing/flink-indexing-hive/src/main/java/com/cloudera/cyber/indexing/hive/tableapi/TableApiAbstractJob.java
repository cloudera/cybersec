package com.cloudera.cyber.indexing.hive.tableapi;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.hive.FilterMapFunction;
import com.cloudera.cyber.indexing.hive.util.FlinkSchemaUtil;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Streams;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.FormatDescriptor;
import org.apache.flink.table.api.Schema;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.TableDescriptor;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
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


public abstract class TableApiAbstractJob {

    private static final String TABLES_INIT_FILE_PARAM = "flink.tables-init-file";
    private static final String MAPPING_FILE_PARAM = "flink.mapping-file";
    protected static final String KAFKA_TABLE = "KafkaTempView";

    protected static final String BASE_COLUMN_MAPPING_JSON = "base-column-mapping.json";

    private final List<MappingColumnDto> defaultMappingList;
    private final List<TableColumnDto> defaultColumnList;
    protected final DataStream<ScoredMessage> source;
    protected final StreamExecutionEnvironment env;
    protected final ParameterTool params;
    protected final String connectorName;

    public TableApiAbstractJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source,
                               String connectorName, String baseTableJson) throws IOException {
        this.params = params;
        this.env = env;
        this.source = source;
        this.connectorName = connectorName;
        defaultMappingList = Utils.readResourceFile(BASE_COLUMN_MAPPING_JSON, getClass(),
                new TypeReference<List<MappingColumnDto>>() {
                });
        defaultColumnList = Utils.readResourceFile(baseTableJson, getClass(),
                new TypeReference<List<TableColumnDto>>() {
                });
    }

    public StreamExecutionEnvironment startJob() throws Exception {
        System.out.println("Creating Table env...");
        final StreamTableEnvironment tableEnv = getTableEnvironment();

        System.out.println("Configuring Table env...");
        configure(tableEnv);

        System.out.printf("Registering %s catalog... %s%n", connectorName, String.join(", ", tableEnv.listCatalogs()));
        registerCatalog(tableEnv);

        System.out.println("Getting tables config...");
        final Map<String, List<TableColumnDto>> tablesConfig = getTablesConfig();

        System.out.println("Creating tables...");
        final Set<String> tableList = getExistingTableList(tableEnv);
        setConnectorDialect(tableEnv);

        tablesConfig.forEach(
                (tableName, columnList) -> createTableIfNotExists(tableEnv, tableList, tableName, columnList));
        tableEnv.getConfig().setSqlDialect(SqlDialect.DEFAULT);

        System.out.println("Getting topic mapping...");
        final Map<String, MappingDto> topicMapping = getTopicMapping();

        System.out.println("Creating Kafka table...");
        createKafkaTable(tableEnv);

        System.out.printf("Executing %s insert...%n", connectorName);
        executeInsert(tableEnv, topicMapping, tablesConfig);

        System.out.println("TableApiJob is done!");
        return jobReturnValue();
    }

    protected HashSet<String> getExistingTableList(StreamTableEnvironment tableEnv) {
        return new HashSet<>(Arrays.asList(tableEnv.listTables()));
    }

    protected StreamExecutionEnvironment jobReturnValue() {
        return env;
    }

    protected void executeInsert(StreamTableEnvironment tableEnv, Map<String, MappingDto> topicMapping, Map<String, List<TableColumnDto>> tablesConfig) {
        System.out.printf("Filling Insert statement list...%s%n", Arrays.toString(tableEnv.listTables()));
        final StreamStatementSet insertStatementSet = tableEnv.createStatementSet();

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
    }

    protected abstract void registerCatalog(StreamTableEnvironment tableEnv);

    protected void setConnectorDialect(StreamTableEnvironment tableEnv) {
        //to be overwritten if needed
    }

    protected void createTableIfNotExists(StreamTableEnvironment tableEnv, Set<String> tableList, String tableName, List<TableColumnDto> columnList) {
        if (tableList.contains(tableName)) {
            System.out.printf("%s table [%s] already exists. Skipping its creation.%n", connectorName, tableName);
        } else {
            System.out.printf("Creating %s table %s...%n", connectorName, tableName);
            final Schema schema = FlinkSchemaUtil.buildTableSchema(columnList);
            final TableDescriptor tableDescriptor = buildTableDescriptor(schema);
            try {
                System.out.printf("Creating %s table %s: %s%n", connectorName, tableName, tableDescriptor);
                tableEnv.createTable(tableName, tableDescriptor);
            } catch (Exception e) {
                System.err.printf("Error creating the %s: %s%n", connectorName, tableDescriptor);
                throw e;
            }
            System.out.printf("%s table created: %s%n", connectorName, tableName);
        }
    }

    private TableDescriptor buildTableDescriptor(Schema schema) {
        return fillTableOptions(TableDescriptor
                .forConnector(getTableConnector())
                .schema(schema)
                .partitionedBy("dt", "hr")
                .format(getFormatDescriptor()))
                .build();
    }

    protected abstract String getTableConnector();

    protected abstract FormatDescriptor getFormatDescriptor();


    protected TableDescriptor.Builder fillTableOptions(TableDescriptor.Builder builder) {
        return builder
                .option("partition.time-extractor.timestamp-pattern", "$dt $hr:00:00")
                .option("sink.partition-commit.trigger", "process-time")
                .option("sink.partition-commit.delay", "1 h")
                .option("sink.partition-commit.policy.kind", "metastore,success-file");
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

    private void configure(StreamTableEnvironment tableEnv) {
        final HashMap<String, String> conf = new HashMap<>();
        conf.put("pipeline.name", params.get("flink.job.name", String.format("Indexing - %s TableApi", connectorName)));
        tableEnv.getConfig().addConfiguration(Configuration.fromMap(conf));
        tableEnv.createTemporarySystemFunction("filterMap", FilterMapFunction.class);
    }

    protected final String buildInsertSql(String topic, MappingDto mappingDto) {
        return String.join("\n", getInsertSqlPrefix() + " " + mappingDto.getTableName() + "(" + getInsertColumns(mappingDto) + ") " + getInsertSqlSuffix(),
                " SELECT " + getFromColumns(mappingDto),
                " from " + KAFKA_TABLE,
                String.format(" where `source`='%s'", topic));
    }

    protected String getInsertSqlPrefix() {
        return "INSERT INTO ";
    }

    protected String getInsertSqlSuffix() {
        return "";
    }

    protected String getInsertColumns(MappingDto mappingDto) {
        return mappingDto.getColumnMapping().stream()
                .map(MappingColumnDto::getName)
                .collect(Collectors.joining(", ", " ", " "));
    }

    private String getFromColumns(MappingDto mappingDto) {
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
                        fullPath = String.join(".", fullPath.split("\\."));
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

    protected Map<String, MappingDto> getTopicMapping() throws IOException {
        TypeReference<HashMap<String, MappingDto>> typeRef
                = new TypeReference<HashMap<String, MappingDto>>() {
        };
        final HashMap<String, MappingDto> columnMappingMap = Utils.readFile(params.getRequired(MAPPING_FILE_PARAM), typeRef);

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

    protected Map<String, List<TableColumnDto>> getTablesConfig() throws IOException {
        TypeReference<HashMap<String, List<TableColumnDto>>> typeRef
                = new TypeReference<HashMap<String, List<TableColumnDto>>>() {
        };
        final String filePath = params.get(TABLES_INIT_FILE_PARAM);
        if (filePath == null) {
            return Collections.emptyMap();
        }
        final HashMap<String, List<TableColumnDto>> columnMap = Utils.readFile(filePath, typeRef);
        final List<TableColumnDto> partitionColumns = Arrays.asList(TableColumnDto.builder()
                .name("dt")
                .type("string")
                .build(), TableColumnDto.builder()
                .name("hr")
                .type("string")
                .build());
        //adding the default columns to each table
        columnMap.forEach((tableName, columnList) -> {
            final List<TableColumnDto> customColumns = Optional.ofNullable(columnList)
                    .orElse(Collections.emptyList());
            final Map<String, TableColumnDto> combinedColumnMap = Streams.concat(customColumns.stream(), defaultColumnList.stream(), partitionColumns.stream())
                    .collect(Collectors.toMap(TableColumnDto::getName, Function.identity(), (f, s) -> f, LinkedHashMap::new));
            //partition columns should be placed last
            partitionColumns.forEach(col -> combinedColumnMap.put(col.getName(), col));

            columnMap.put(tableName, new ArrayList<>(combinedColumnMap.values()));
        });
        return columnMap;
    }

}
