package com.cloudera.cyber.indexing.hive.tableapi;

import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.hive.FilterMapFunction;
import com.cloudera.cyber.scoring.ScoredMessage;
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
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
public abstract class TableApiAbstractJob {

    protected static final String KAFKA_TABLE = "KafkaTempView";

    protected final DataStream<ScoredMessage> source;
    protected final StreamExecutionEnvironment env;
    protected final ParameterTool params;

    public TableApiAbstractJob(ParameterTool params, StreamExecutionEnvironment env, DataStream<ScoredMessage> source) {
        this.params = params;
        this.env = env;
        this.source = source;
    }

    public void startJob() throws Exception {
        System.out.println("Creating Table env...");
        final StreamTableEnvironment tableEnv = getTableEnvironment();

        System.out.println("Configuring Table env...");
        configure(tableEnv);

        System.out.printf("Registering Hive catalog... %s%n", String.join(", ", tableEnv.listCatalogs()));
        registerHiveCatalog(tableEnv);

        System.out.println("Getting tables config...");
        final Map<String, List<TableColumnDto>> tablesConfig = getTablesConfig();

        System.out.println("Creating tables...");
        final Set<String> tableList = new HashSet<>(Arrays.asList(tableEnv.listTables()));
        setConnectorDialect(tableEnv);

        tablesConfig.forEach(
                (tableName, columnList) -> createTableIfNotExists(tableEnv, tableList, tableName, columnList));
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

    protected abstract Map<String, List<TableColumnDto>> getTablesConfig() throws IOException;

    protected void setConnectorDialect(StreamTableEnvironment tableEnv){
        //to be overwritten if needed
    }

    protected abstract void createTableIfNotExists(StreamTableEnvironment tableEnv, Set<String> tableList, String tableName, List<TableColumnDto> columnList);

    protected abstract Map<String, MappingDto> getTopicMapping() throws IOException;

    protected abstract String getInsertColumns(MappingDto mappingDto);

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

    private void configure(StreamTableEnvironment tableEnv) {
        final HashMap<String, String> conf = new HashMap<>();
        conf.put("pipeline.name", params.get("flink.job.name", "Indexing - Hive TableApi"));
        tableEnv.getConfig().addConfiguration(Configuration.fromMap(conf));
        tableEnv.createTemporarySystemFunction("filterMap", FilterMapFunction.class);
    }

    private String buildInsertSql(String topic, MappingDto mappingDto) {
        return String.join("\n", "insert into " + mappingDto.getHiveTable() + "(" + getInsertColumns(mappingDto) + ")",
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

}
