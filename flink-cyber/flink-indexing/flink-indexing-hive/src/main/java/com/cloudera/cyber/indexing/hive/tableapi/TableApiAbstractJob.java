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
import java.io.IOException;
import java.util.*;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.*;
import org.apache.flink.table.api.bridge.java.StreamStatementSet;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.catalog.Column;
import org.apache.flink.table.catalog.ResolvedSchema;
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalTypeFamily;
import org.apache.flink.types.Row;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


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
    final Map<String, List<TableColumnDto>> rawTablesConfig = getRawTablesConfig();
    final Map<String, List<TableColumnDto>> tablesConfig = appendDefaultTablesConfig(rawTablesConfig);

    System.out.println("Creating tables...");
    setConnectorDialect(tableEnv);

    final Map<String, ResolvedSchema> tableSchemaMap = createTables(tableEnv, tablesConfig);

    System.out.println("Getting topic mapping...");
    final Map<String, MappingDto> topicMapping = getTopicMapping();

    System.out.println("Validating output tables mappings...");
    validateMappings(tableSchemaMap, topicMapping);

    tableEnv.getConfig().setSqlDialect(SqlDialect.DEFAULT);

    System.out.println("Creating Kafka table...");
    createKafkaTable(tableEnv);

    System.out.printf("Executing %s insert...%n", connectorName);
    executeInsert(tableEnv, topicMapping, tablesConfig, tableSchemaMap);

    System.out.println("TableApiJob is done!");
    return jobReturnValue();
  }

  protected boolean isNonDefaultColumn(String columnName) {
    return defaultColumnList.stream().noneMatch(c -> c.getName().equals(columnName));
  }

  /**
   * Creates tables in the tableEnv based on the tablesConfig. If table already exists, its schema is fetched.
   *
   * @param tableEnv     is the Flink environment in which the tables are going to be created.
   * @param tablesConfig modified version of rawTablesConfig that contains default and partition columns.
   * @return Map with table name as a key and table schema as a value.
   */
  private Map<String, ResolvedSchema> createTables(StreamTableEnvironment tableEnv,
                                                   Map<String, List<TableColumnDto>> tablesConfig) {
    final Set<String> tableList = getExistingTableList(tableEnv);

    return tablesConfig.entrySet().stream()
        .collect(Collectors.toMap(Entry::getKey,
            entry -> createTableIfNotExists(tableEnv, tableList, entry.getKey(), entry.getValue())));
  }

  protected void validateMappings(Map<String, ResolvedSchema> tableSchemaMap,
                                  Map<String, MappingDto> topicMapping) {
    for (Entry<String, MappingDto> entry : topicMapping.entrySet()) {
      final String source = entry.getKey();
      final MappingDto mappingDto = entry.getValue();
      final String tableName = mappingDto.getTableName();

      if (!StringUtils.hasText(tableName)) {
        throw new RuntimeException(String.format("Provided empty table name for the [%s] source!", source));
      }

      final ResolvedSchema schema = tableSchemaMap.get(tableName);
      if (schema == null) {
        throw new RuntimeException(String.format("Table [%s] is not found!", tableName));
      }

      final Set<String> tableColumns = schema.getColumns().stream()
              .map(Column::getName)
              .collect(Collectors.toSet());

      final List<String> invalidColumnList = mappingDto.getColumnMapping().stream()
              .map(MappingColumnDto::getName)
              .filter(columnName -> !StringUtils.hasText(columnName) || !tableColumns.contains(columnName))
              .collect(Collectors.toList());
      if (!invalidColumnList.isEmpty()) {
        throw new RuntimeException(
                String.format(
                        "Found invalid column mappings for source [%s]. Those columns are either not present in the table config or have empty names: %s",
                        source, invalidColumnList));
      }

      validateTransformations(source, schema, mappingDto);
    }
  }

  private void validateTransformations(String source, ResolvedSchema tableSchema, MappingDto mappingDto) {

    final Map<String, DataType> tableColumnMap = tableSchema.getColumns().stream()
            .collect(Collectors.toMap(Column::getName, Column::getDataType));

    final List<String> columnListWithoutTransformation = mappingDto.getColumnMapping().stream()
            .map(mapping -> {
              final String columnName = mapping.getName();
              final DataType tableColumnDataType = tableColumnMap.get(columnName);

              if (DataTypes.STRING().equals(tableColumnDataType)) {
                return null;
              }

              if (StringUtils.hasText(getTransformation(tableColumnDataType, mapping))) {
                return null;
              }

              return columnName;
            })
            .filter(Objects::nonNull)
            .filter(this::isNonDefaultColumn)
            .collect(Collectors.toList());

    if (!columnListWithoutTransformation.isEmpty()) {
      throw new RuntimeException(
              String.format(
                      "Found column mappings of non-string type without transformations for source [%s]: %s",
                      source, columnListWithoutTransformation));
    }
  }


  protected HashSet<String> getExistingTableList(StreamTableEnvironment tableEnv) {
    return new HashSet<>(Arrays.asList(tableEnv.listTables()));
  }

  protected StreamExecutionEnvironment jobReturnValue() {
    return env;
  }

  protected void executeInsert(StreamTableEnvironment tableEnv, Map<String, MappingDto> topicMapping,
                               Map<String, List<TableColumnDto>> tablesConfig, Map<String, ResolvedSchema> tableSchemaMap) {
    System.out.printf("Filling Insert statement list...%s%n", Arrays.toString(tableEnv.listTables()));
    final StreamStatementSet insertStatementSet = tableEnv.createStatementSet();

    topicMapping.forEach((topic, mappingDto) -> {
      ResolvedSchema tableSchema = tableSchemaMap.get(mappingDto.getTableName());
      final String insertSql = buildInsertSql(topic, mappingDto, tableSchema);
      try {
        insertStatementSet.addInsertSql(insertSql);
        System.out.printf("Insert SQL added to the queue for the table: %s%nSQL: %s%n", mappingDto.getTableName(),
            insertSql);
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

  private ResolvedSchema createTableIfNotExists(StreamTableEnvironment tableEnv, Set<String> tableList,
                                                String tableName, List<TableColumnDto> columnList) {
    if (tableList.contains(tableName)) {
      return handleExistingTable(tableEnv, tableName, columnList);
    }

    return createTable(tableEnv, tableName, columnList);
  }

  protected String getTransformation(DataType tableColumnDataType, MappingColumnDto mapping) {
    String transformation = mapping.getTransformation();
    if (transformation == null && isNonDefaultColumn(mapping.getName()) && (DataTypes.BOOLEAN().equals(tableColumnDataType) || tableColumnDataType.getLogicalType().is(LogicalTypeFamily.NUMERIC))) {
      transformation = String.format("TRY_CAST(%%s AS %s)", tableColumnDataType.getLogicalType().getTypeRoot().name());
    }

    return transformation;
  }

  protected ResolvedSchema createTable(StreamTableEnvironment tableEnv, String tableName,
                                       List<TableColumnDto> columnList) {
    System.out.printf("Creating %s table %s...%n", connectorName, tableName);
    final ResolvedSchema resolvedSchema = FlinkSchemaUtil.getResolvedSchema(columnList);
    final Schema schema = FlinkSchemaUtil.buildSchema(resolvedSchema);
    final TableDescriptor tableDescriptor = buildTableDescriptor(schema);
    try {
      System.out.printf("Creating %s table %s: %s%n", connectorName, tableName, tableDescriptor);
      tableEnv.createTable(tableName, tableDescriptor);
    } catch (Exception e) {
      System.err.printf("Error creating the %s: %s%n", connectorName, tableDescriptor);
      throw e;
    }
    System.out.printf("%s table created: %s%n", connectorName, tableName);
    return resolvedSchema;
  }

  protected ResolvedSchema handleExistingTable(StreamTableEnvironment tableEnv, String tableName,
      List<TableColumnDto> columnList) {
    final ResolvedSchema existingTableSchema = tableEnv.from(tableName).getResolvedSchema();
    if (CollectionUtils.isEmpty(columnList)) {
      System.out.printf("%s table [%s] already exists and no table config provided. Skipping its creation.%n",
          connectorName,
          tableName);
      return existingTableSchema;
    }

    final List<Column> existingColumns = existingTableSchema.getColumns();
    final List<Column> configColumns = FlinkSchemaUtil.getResolvedSchema(columnList).getColumns();
    for (Column configColumn : configColumns) {
      final DataType dataType = configColumn.getDataType();
      final String columnName = configColumn.getName();
      final boolean matchingColumnExists = existingColumns.stream()
          .anyMatch(c -> c.getName().equals(columnName) && c.getDataType().equals(dataType));
      if (!matchingColumnExists) {
        throw new RuntimeException(String.format(
            "%s table [%s] already exists, but table config was provided for it as well. "
            + "In this case all columns from table config should be present in the existing table, "
            + "but we didn't find the [%s] column of type [%s].",
            connectorName, tableName, columnName, dataType));
      }
    }
    System.out.printf("%s table [%s] already exists and provided table config matches it. Skipping its creation.%n",
        connectorName,
        tableName);
    return existingTableSchema;
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

  protected final String buildInsertSql(String topic, MappingDto mappingDto, ResolvedSchema tableSchema) {
    return String.join("\n",
        getInsertSqlPrefix() + " " + mappingDto.getTableName() + "(" + getInsertColumns(mappingDto) + ") "
        + getInsertSqlSuffix(),
        " SELECT " + getFromColumns(mappingDto, tableSchema),
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

  private String getFromColumns(MappingDto mappingDto, ResolvedSchema tableSchema) {
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

          Optional<Column> column = tableSchema.getColumn(mappingColumnDto.getName());
          final String transformation = column.map(value -> getTransformation(value.getDataType(), mappingColumnDto)).orElse("");

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
    final HashMap<String, MappingDto> columnMappingMap = Utils.readFile(params.getRequired(MAPPING_FILE_PARAM),
        typeRef);

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

  /**
   * Method provides contents of the tables config file.
   *
   * @return Map with table name as a key, and a list of columns as a value.
   * @throws IOException in case it can't read the tables config file.
   */
  protected Map<String, List<TableColumnDto>> getRawTablesConfig() throws IOException {
    TypeReference<HashMap<String, List<TableColumnDto>>> typeRef
        = new TypeReference<HashMap<String, List<TableColumnDto>>>() {
    };
    final String filePath = params.get(TABLES_INIT_FILE_PARAM);
    if (filePath == null) {
      return Collections.emptyMap();
    }
    return Utils.readFile(filePath, typeRef);
  }

  /**
   * Method appends default and partition columns to the raw tables config.
   *
   * @return Map with table name as a key, and a list of columns as a value.
   */
  protected Map<String, List<TableColumnDto>> appendDefaultTablesConfig(
      Map<String, List<TableColumnDto>> rawTablesConfig) {
    if (rawTablesConfig == null || rawTablesConfig.isEmpty()) {
      return Collections.emptyMap();
    }
    final Map<String, List<TableColumnDto>> columnMap = new HashMap<>(rawTablesConfig);
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
      final Map<String, TableColumnDto> combinedColumnMap = Streams.concat(customColumns.stream(),
              defaultColumnList.stream(), partitionColumns.stream())
          .collect(Collectors.toMap(TableColumnDto::getName, Function.identity(), (f, s) -> f, LinkedHashMap::new));
      //partition columns should be placed last
      partitionColumns.forEach(col -> combinedColumnMap.put(col.getName(), col));

      columnMap.put(tableName, new ArrayList<>(combinedColumnMap.values()));
    });
    return columnMap;
  }

}
