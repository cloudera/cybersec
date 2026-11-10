package com.cloudera.cyber.indexing.hive.tableapi;

import com.cloudera.cyber.indexing.MappingColumnDto;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.cyber.indexing.TableDto;
import com.cloudera.cyber.indexing.hive.FilterMapFunction;
import com.cloudera.cyber.indexing.hive.util.FlinkSchemaUtil;
import com.cloudera.cyber.scoring.ScoredMessage;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
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
import org.apache.flink.table.types.DataType;
import org.apache.flink.table.types.logical.LogicalTypeFamily;
import org.apache.flink.types.Row;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;


public abstract class TableApiAbstractJob {

  protected static final String KAFKA_TABLE = "KafkaTempView";

  protected final DataStream<ScoredMessage> source;
  protected final StreamExecutionEnvironment env;
  protected StreamTableEnvironment tableEnv;
  protected final ParameterTool params;
  protected final String connectorName;
  protected final FlinkSchemaUtil.SerializationFormat serializationFormat;
  protected final IndexTableMappingConfiguration configuration;

  public TableApiAbstractJob(ParameterTool params, StreamExecutionEnvironment env, StreamTableEnvironment tableEnv, DataStream<ScoredMessage> source,
                             String connectorName, String baseTableJson, FlinkSchemaUtil.SerializationFormat serializationFormat) throws IOException {
   this(params, env, tableEnv, source, connectorName, serializationFormat, new IndexTableMappingConfiguration(baseTableJson));
  }

  public TableApiAbstractJob(ParameterTool params, StreamExecutionEnvironment env, StreamTableEnvironment tableEnv, DataStream<ScoredMessage> source,
                             String connectorName, FlinkSchemaUtil.SerializationFormat serializationFormat,
                             IndexTableMappingConfiguration configuration) {
    this.params = params;
    this.env = env;
    this.tableEnv = tableEnv;
    this.source = source;
    this.connectorName = connectorName;
    this.serializationFormat = serializationFormat;
    this.configuration = configuration;
  }

  /**
   * Registers tables and inserts, then submits them as a standalone Flink job.
   *
   * <p>Use {@link #startJob(boolean)} with {@code executeInserts = false} when the inserts should
   * instead be attached to the caller's {@link StreamExecutionEnvironment} so that a single {@code
   * env.execute()} submits the whole pipeline as one job.
   */
  public StreamExecutionEnvironment startJob() throws Exception {
    return startJob(true);
  }

  /**
   * @param executeInserts when {@code true} the insert statement set is submitted as its own job;
   *                       when {@code false} it is attached to the environment without executing,
   *                       so the caller's single {@code env.execute()} runs the whole pipeline.
   */
  public StreamExecutionEnvironment startJob(boolean executeInserts) throws Exception {
    return startJob(executeInserts, true, null);
  }

  /**
   * @param executeInserts when {@code true} the insert statement set is submitted as its own job;
   *                       when {@code false} it is attached to the environment without executing.
   * @param writeUnenrichedTables when {@code true} the derived unenriched tables are written in
   *                              addition to the primary tables; when {@code false} only the
   *                              primary tables are written.
   * @param sourcesToWrite restricts the tables written to these sources; {@code null} writes every
   *                       source defined in the mapping configuration.
   */
  public StreamExecutionEnvironment startJob(boolean executeInserts, boolean writeUnenrichedTables, Set<String> sourcesToWrite) throws Exception {

    createTableEnvIfNeeded();

    System.out.println("Configuring Table env...");
    configure(tableEnv);

    System.out.printf("Registering %s catalog... %s%n", connectorName, String.join(", ", tableEnv.listCatalogs()));
    registerCatalog(tableEnv);

    this.configuration.readConfiguration(params, null);

    // Restrict what is written to the requested sources and, optionally, drop the derived unenriched tables.
    final Map<String, MappingDto> topicMapping = selectMappings(configuration.getTopicMapping(), sourcesToWrite);
    final Map<String, MappingDto> unenrichedTopicMapping = writeUnenrichedTables
            ? selectMappings(configuration.getUnenrichedTopicMapping(), sourcesToWrite)
            : Collections.emptyMap();
    // Only trim the table config when the write scope is restricted, so the indexing job keeps
    // creating every table declared in its config.
    final Map<String, TableDto> tablesConfig = sourcesToWrite == null
            ? configuration.getTablesConfig()
            : selectTables(configuration.getTablesConfig(), topicMapping, unenrichedTopicMapping);

    System.out.println("Creating tables...");
    setConnectorDialect(tableEnv);

    final Map<String, ResolvedSchema> tableSchemaMap = createTables(tableEnv, tablesConfig);

    System.out.println("Validating output tables mappings...");
    validateMappings(tableSchemaMap, topicMapping);
    validateMappings(tableSchemaMap, unenrichedTopicMapping);

    tableEnv.getConfig().setSqlDialect(SqlDialect.DEFAULT);

    System.out.println("Creating Kafka table...");
    createKafkaTable(tableEnv);

    System.out.printf("Executing %s insert...%n", connectorName);

    createInserts(tableEnv, topicMapping, tablesConfig, unenrichedTopicMapping, tableSchemaMap, executeInserts);

    System.out.println("TableApiJob is done!");
    return jobReturnValue();
  }

  /**
   * Restricts mappings to the requested sources.  A null {@code sourcesToWrite} keeps every source.
   */
  Map<String, MappingDto> selectMappings(Map<String, MappingDto> mappings, Set<String> sourcesToWrite) {
    if (sourcesToWrite == null) {
      return mappings;
    }
    return mappings.entrySet().stream()
            .filter(entry -> sourcesToWrite.contains(entry.getKey()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }

  /**
   * Keeps only the tables that are referenced by the mappings that are going to be written.
   */
  Map<String, TableDto> selectTables(Map<String, TableDto> tablesConfig, Map<String, MappingDto> topicMapping,
                                     Map<String, MappingDto> unenrichedTopicMapping) {
    final Set<String> tableNames = new HashSet<>();
    topicMapping.values().forEach(mapping -> tableNames.add(mapping.getTableName()));
    unenrichedTopicMapping.values().forEach(mapping -> tableNames.add(mapping.getTableName()));
    return tablesConfig.entrySet().stream()
            .filter(entry -> tableNames.contains(entry.getKey()))
            .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
  }


  /**
   * Creates tables in the tableEnv based on the tablesConfig. If table already exists, its schema is fetched.
   *
   * @param tableEnv     is the Flink environment in which the tables are going to be created.
   * @param tablesConfig modified version of rawTablesConfig that contains default and partition columns.
   * @return Map with table name as a key and table schema as a value.
   */
  private Map<String, ResolvedSchema> createTables(StreamTableEnvironment tableEnv,
                                                   Map<String, TableDto> tablesConfig) {
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
              .toList();
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
            .filter(configuration::isNonDefaultColumn)
            .toList();

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

  protected void createInserts(StreamTableEnvironment tableEnv, Map<String, MappingDto> topicMapping,
                               Map<String, TableDto> tablesConfig, Map<String, MappingDto> unenrichedTopicMapping, Map<String, ResolvedSchema> tableSchemaMap) {
    createInserts(tableEnv, topicMapping, tablesConfig, unenrichedTopicMapping, tableSchemaMap, true);
  }

  /**
   * Registers the insert pipelines for each mapped topic.
   *
   * @param executeInserts when {@code true} the statement set is executed as its own job; when
   *                       {@code false} it is attached to the environment so the caller's {@code
   *                       env.execute()} submits everything as a single job.
   */
  protected void createInserts(StreamTableEnvironment tableEnv, Map<String, MappingDto> topicMapping,
                               Map<String, TableDto> tablesConfig, Map<String, MappingDto> unenrichedTopicMapping, Map<String, ResolvedSchema> tableSchemaMap,
                               boolean executeInserts) {
    if (!topicMapping.isEmpty()) {
      System.out.printf("Filling Insert statement list...%s%n", Arrays.toString(tableEnv.listTables()));
      final StreamStatementSet insertStatementSet = tableEnv.createStatementSet();

      topicMapping.forEach((topic, mappingDto) -> {
        registerFilterMapFunction(tableEnv, topic, mappingDto, Collections.emptySet());
        addInsertSqlStatement(tableSchemaMap, topic, mappingDto, insertStatementSet);
        Optional.ofNullable(unenrichedTopicMapping.get(topic))
                .ifPresent(unenrichedDto -> {
                  registerFilterMapFunction(tableEnv, topic, unenrichedDto, configuration.getFilterMapIgnorePrefixes(topic));
                  addInsertSqlStatement(tableSchemaMap, topic, unenrichedDto, insertStatementSet);
                });
      });

      if (executeInserts) {
        System.out.println("Executing Insert statement list...");
        insertStatementSet.execute();
      } else {
        // Attach the inserts to the environment instead of executing them, so that the caller's
        // single env.execute() submits the whole pipeline (source, processing and inserts) as one job.
        System.out.println("Attaching Insert statement list to the environment...");
        insertStatementSet.attachAsDataStream();
      }
    }
  }

  protected void registerFilterMapFunction(StreamTableEnvironment tableEnv, String source, MappingDto mappingDto, Set<String> ignorePrefixes) {
    String filterMapFunctionName = getFilterMapFunctionName(source, mappingDto.getTableName());
    Set<String> ignoreFields = mappingDto.getIgnoreFields();
    System.out.printf("Registering %s\n", filterMapFunctionName);
    System.out.printf("Ignoring fields %s\n", ignoreFields);
    System.out.printf("Ignore prefixes %s\n", ignorePrefixes);

    tableEnv.createTemporarySystemFunction(filterMapFunctionName,
        new FilterMapFunction(mappingDto.getIgnoreFields(), ignorePrefixes));
  }

  protected String getFilterMapFunctionName(String source, String destTableName) {
    // create a unique filter for this source and destination Table.  Replace any special characters that
    // are not legal in a function name with an underscore.  Prevents failures from invalid sql.
    return String.format("filterMap%s%s", source, destTableName).replaceAll("[^a-zA-Z0-9_]", "");
  }

  private void addInsertSqlStatement(Map<String, ResolvedSchema> tableSchemaMap, String topic, MappingDto mappingDto, StreamStatementSet insertStatementSet) {
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
  }

  protected abstract void registerCatalog(StreamTableEnvironment tableEnv);

  protected void setConnectorDialect(StreamTableEnvironment ignored) {
    //to be overwritten if needed
  }

  private ResolvedSchema createTableIfNotExists(StreamTableEnvironment tableEnv, Set<String> tableList,
                                                String tableName, TableDto tableDto) {
    List<TableColumnDto> columnList = tableDto.getColumns();
    if (tableList.contains(tableName)) {
      return handleExistingTable(tableEnv, tableName, columnList);
    }

    return createTable(tableEnv, tableName, columnList);
  }

  protected String getTransformation(DataType tableColumnDataType, MappingColumnDto mapping) {
    String transformation = mapping.getTransformation();
    if (transformation == null && configuration.isNonDefaultColumn(mapping.getName())
            && (DataTypes.BOOLEAN().equals(tableColumnDataType)
            || tableColumnDataType.getLogicalType().is(LogicalTypeFamily.NUMERIC))) {
      transformation = String.format("TRY_CAST(%%s AS %s)", tableColumnDataType.getLogicalType().getTypeRoot().name());
    }

    return transformation;
  }

  protected ResolvedSchema createTable(StreamTableEnvironment tableEnv, String tableName,
                                       List<TableColumnDto> columnList) {
    final ResolvedSchema resolvedSchema = FlinkSchemaUtil.getResolvedSchema(columnList, serializationFormat);
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
    ResolvedSchema existingTableSchema = tableEnv.from(tableName).getResolvedSchema();
    if (CollectionUtils.isEmpty(columnList)) {
      System.out.printf("%s table [%s] already exists and no table config provided. Skipping its creation.%n",
          connectorName,
          tableName);
      return existingTableSchema;
    }

    final List<Column> existingColumns = existingTableSchema.getColumns();
    final List<Column> configColumns = FlinkSchemaUtil.getResolvedSchema(columnList, serializationFormat).getColumns();
    final List<Column> missingColumns = new ArrayList<>();
    for (Column configColumn : configColumns) {
      final DataType dataType = configColumn.getDataType();
      final String columnName = configColumn.getName();
      final boolean matchingColumnExists = existingColumns.stream()
          .anyMatch(c -> c.getName().equals(columnName) && c.getDataType().equals(dataType));
      if (!matchingColumnExists) {
        missingColumns.add(configColumn);
      }
    }
    if (!missingColumns.isEmpty()) {
      existingTableSchema = handleMissingColumns(tableEnv, connectorName, tableName, missingColumns);
    } else {
      System.out.printf("%s table [%s] already exists and provided table config matches it. Skipping its creation.%n",
              connectorName,
              tableName);
    }
    return existingTableSchema;
  }

  protected ResolvedSchema handleMissingColumns(StreamTableEnvironment tableEnv, String connectorName, String tableName, List<Column> missingColumns) {

      String missingColumnsMessage = missingColumns.stream().map(col -> String.join(":", col.getName(), col.getDataType().toString())).collect(Collectors.joining(","));
      throw new RuntimeException(String.format(
              "%s table [%s] already exists, but table config was provided for it as well. "
                      + "In this case all columns from table config should be present in the existing table, "
                      + "but we didn't find the columns [%s].",
              connectorName, tableName, missingColumnsMessage));
  }

  private TableDescriptor buildTableDescriptor(Schema schema) {

      TableDescriptor.Builder tableDescriptorBuilder = TableDescriptor
              .forConnector(getTableConnector())
              .schema(schema)
              .partitionedBy("dt", "hr");
      FormatDescriptor formatDescriptor = getFormatDescriptor();
      if (formatDescriptor != null) {
          tableDescriptorBuilder.format(formatDescriptor);
      }
    return fillTableOptions(tableDescriptorBuilder).build();
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

  private void createTableEnvIfNeeded() {
    if (tableEnv != null) {
      System.out.println("Using exising Table env...");
    } else {
      System.out.println("Creating Table env...");
      this.tableEnv = StreamTableEnvironment.create(env);
    }
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
  }

  protected final String buildInsertSql(String source, MappingDto mappingDto, ResolvedSchema tableSchema) {
    return String.join("\n",
        getInsertSqlPrefix() + " " + getTableName(source, mappingDto) + "(" + getInsertColumns(mappingDto) + ") "
                    + getInsertSqlSuffix(),
            " SELECT " + getFromColumns(source, mappingDto, tableSchema),
            " from " + KAFKA_TABLE,
            String.format(" where `source`='%s'", source));
  }

  protected String getTableName(String source, MappingDto mappingDto) {
    return mappingDto.getTableName();
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

  private String getFromColumns(String source, MappingDto mappingDto, ResolvedSchema tableSchema) {
    System.out.println("Building from columns");
    final String filterMapFunctionName = getFilterMapFunctionName(source, mappingDto.getTableName());
    return mappingDto.getColumnMapping().stream()
        .map(mappingColumnDto -> {
          final List<String> kafkaNameList = mappingColumnDto.getKafkaNameList();
          final String path = mappingColumnDto.getPath();

          List<String> fullPathList = kafkaNameList.stream()
                                              .map(kafkaName -> {
                                                String fullPath;
                                                if (path.startsWith("..")) {
                                                  fullPath = path.substring(2);
                                                } else {
                                                  fullPath = String.format("message.%s", path);
                                                }
                                                if (StringUtils.hasText(fullPath)) {
                                                  fullPath = String.join(".", fullPath.split("\\."));
                                                }

                                                return "(" + fullPath + kafkaName + ")";
                                              }).collect(Collectors.toList());

          Optional<Column> column = tableSchema.getColumn(mappingColumnDto.getName());
          String transformation = column.map(value -> getTransformation(value.getDataType(), mappingColumnDto)).orElse("");

          if (StringUtils.hasText(transformation)) {
            transformation = transformation.replace("filterMap", filterMapFunctionName);
          }

          return StringUtils.hasText(transformation)
                ? String.format(transformation, fullPathList.toArray(new Object[0]))
                : String.join(", ", fullPathList);
        })
        .collect(Collectors.joining(", ", " ", " "));
  }

}
