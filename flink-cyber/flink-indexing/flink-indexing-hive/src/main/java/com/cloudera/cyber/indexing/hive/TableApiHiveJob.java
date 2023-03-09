package com.cloudera.cyber.indexing.hive;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.hive.dto.HiveColumnDto;
import com.cloudera.cyber.indexing.hive.dto.MappingColumnDto;
import com.cloudera.cyber.indexing.hive.dto.MappingDto;
import com.cloudera.cyber.scoring.ScoredMessage;
import com.fasterxml.jackson.core.type.TypeReference;
import com.google.common.collect.Streams;
import groovy.util.logging.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.SqlDialect;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.hive.HiveCatalog;
import org.apache.flink.types.Row;
import org.springframework.util.StringUtils;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
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
    private static final String BASE_HIVE_TABLE_JSON = "base-hive-table.json";
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
                System.out.printf("Insert SQL added to the queue for the table: %s%nSQL: %s%n", mappingDto.getHiveTable(), insertSql);
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
            final String ddl = buildHiveTableDLL(hiveTableName, columnList);
            try {
                tableEnv.executeSql(ddl);
            } catch (Exception e) {
                System.err.printf("Error executing the Hive DDL: %s%n", ddl);
                throw e;
            }
            System.out.printf("Hive table created: %s%n", hiveTableName);
        }
    }

    private StreamTableEnvironment getTableEnvironment() {
        final StreamTableEnvironment tableEnv = StreamTableEnvironment.create(env);
        tableEnv.createTemporarySystemFunction("filterMap", FilterMapFunction.class);
        return tableEnv;
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
        //adding the default columns to each table
        columnMap.forEach((tableName, columnList) -> {
            final List<HiveColumnDto> customColumns = Optional.ofNullable(columnList)
                    .orElse(Collections.emptyList());
            final Collection<HiveColumnDto> combinedColumns = Streams.concat(customColumns.stream(), defaultColumnList.stream())
                    .collect(Collectors.toMap(HiveColumnDto::getName, Function.identity(), (f, s) -> f)).values();
            columnMap.put(tableName, new ArrayList<>(combinedColumns));
        });
        return columnMap;
    }

    private void configure(StreamTableEnvironment tableEnv) {
        final HashMap<String, String> conf = new HashMap<>();
        conf.put("pipeline.name", params.get("flink.job.name", "Indexing - Hive TableApi"));
        tableEnv.getConfig().addConfiguration(Configuration.fromMap(conf));
    }

    private String buildInsertSql(String topic, MappingDto mappingDto) {
        return String.join("\n", "insert into " + mappingDto.getHiveTable() + "(" + getHiveInsertColumns(mappingDto) + ")",
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
                                .collect(Collectors.joining("`.`", "`", "`"));
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
                .collect(Collectors.joining("`, `", " `", "` "));
    }

    private String buildHiveTableDLL(String tableName, List<HiveColumnDto> columnList) {
        return String.join("\n", "CREATE TABLE IF NOT EXISTS " + tableName + " ( ",
                getColumnList(columnList),
                ") PARTITIONED BY (",
                " `dt` string, ",
                " `hr` string",
                ") STORED AS parquet TBLPROPERTIES (",
                "  'partition.time-extractor.timestamp-pattern'='$dt $hr:00:00',",
                "  'sink.partition-commit.trigger'='process-time',",
                "  'sink.partition-commit.policy.kind'='metastore,success-file'",
                ")");
    }

    //TODO partitions optimization

    private String getColumnList(List<HiveColumnDto> columnList) {
        return columnList.stream()
                .map(col -> String.format("`%s` %s %s",
                        col.getName(), col.getType(),
                        Optional.ofNullable(col.getNullable()).orElse(true) ? "" : "NOT NULL"))
                .collect(Collectors.joining(", \n", " ", " \n"));
    }


}
