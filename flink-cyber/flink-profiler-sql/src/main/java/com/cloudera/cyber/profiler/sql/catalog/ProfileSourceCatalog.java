package com.cloudera.cyber.profiler.sql.catalog;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.CyberFunctionUtils;
import com.hortonworks.registries.schemaregistry.SchemaVersionInfo;
import com.hortonworks.registries.schemaregistry.client.SchemaRegistryClient;
import com.hortonworks.registries.schemaregistry.errors.SchemaNotFoundException;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.typeutils.RowTypeInfo;
import org.apache.flink.formats.avro.typeutils.AvroSchemaConverter;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.TableSchema;
import org.apache.flink.table.catalog.*;
import org.apache.flink.table.catalog.exceptions.*;
import org.apache.flink.table.catalog.stats.CatalogColumnStatistics;
import org.apache.flink.table.catalog.stats.CatalogTableStatistics;
import org.apache.flink.table.descriptors.Avro;
import org.apache.flink.table.descriptors.Kafka;
import org.apache.flink.table.expressions.Expression;
import org.apache.flink.table.types.utils.TypeConversions;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.KafkaAdminClient;
import org.apache.kafka.clients.admin.ListTopicsOptions;
import org.apache.kafka.clients.admin.ListTopicsResult;

import java.util.*;
import java.util.concurrent.ExecutionException;

import static com.cloudera.cyber.flink.Utils.readKafkaProperties;
import static com.cloudera.cyber.flink.Utils.readSchemaRegistryProperties;

public class ProfileSourceCatalog extends AbstractReadOnlyCatalog {

    private static final String CATALOG_NAME = "profiler";
    private static final String SOURCE_DATABASE = "sources";
    private static final String PROFILE_DATABASE = "profiles";

    private final Map<String, String> properties;
    private AdminClient kafkaClient;
    private final SchemaRegistryClient registryClient;

    // Function repository
    private Map<String, CatalogFunction> functions;

    public ProfileSourceCatalog(Map<String,String> properties) {
        super(CATALOG_NAME, SOURCE_DATABASE);
        this.properties = properties;
        registryClient = new SchemaRegistryClient(
                readSchemaRegistryProperties(properties)
        );
    }

    private Set<String> getSourceTables() throws ExecutionException, InterruptedException {
        ListTopicsResult listTopicsResult = kafkaClient
                .listTopics(new ListTopicsOptions().listInternal(false));
        return listTopicsResult.names().get();
    }

    /**
     * Builds a Flink Schema for the table based on a schema registry avro schema
     *
     * If the schema does not qualify as a source message for profiling, throw IllegalSchemaException
     * and the table will be deemed to not exist.
     *
     * Also handles applying watermarks to the source table for event time handling
     *
     * @param schemaText The JSON String representing the Avro Schema
     * @return TableSchema based on the latest record in Schema Registry
     * @throws IllegalSchemaException If the schema is missing id or ts fields this may be thrown
     * @throws SchemaNotFoundException If the schema does not exist, this will be thrown
     */
    private static TableSchema getTableSchemaForAvro(String schemaText) throws IllegalSchemaException {
        RowTypeInfo rowType = (RowTypeInfo) (TypeInformation<?>) AvroSchemaConverter.convertToTypeInfo(schemaText);

        if (!rowType.getTypeAt("id").getTypeClass().equals(String.class))
            throw new IllegalSchemaException("Schema has no id field");

        if (!rowType.getTypeAt("ts").getTypeClass().equals(Long.class))
            throw new IllegalSchemaException("Schema has no ts timestamp field");

        return new TableSchema.Builder()
                .fields(rowType.getFieldNames(),
                        TypeConversions.fromLegacyInfoToDataType(rowType.getFieldTypes()))
                .primaryKey("id")
                // TODO - Specify a proper watermark here for profile use
                .watermark("ts",
                        "ts - INTERVAL '5' SECOND",
                        DataTypes.TIMESTAMP())
                .build();
    }

    /**
     * Grabs the schema for a given topic
     * @param topic Kafka Topic name
     * @return Table Schema based on the Schema Registry entry for the topic
     * @throws IllegalSchemaException
     * @throws SchemaNotFoundException
     */
    private TableSchema schemaForTopicFromRegistry(String topic) throws IllegalSchemaException, SchemaNotFoundException {
        SchemaVersionInfo schemaInfo = registryClient.getLatestSchemaVersionInfo(topic);
        // translate the Avro Schema to a Table Schema
        return getTableSchemaForAvro(schemaInfo.getSchemaText());
    }

    @Override
    public void open() throws CatalogException {
        kafkaClient = KafkaAdminClient.create(readKafkaProperties(properties, "profiler-catalog", true));

        // initialise the functions
        CyberFunctionUtils.findAll().forEach(c -> {
            functions.put(c.getAnnotation(CyberFunction.class).value(), new CatalogFunctionImpl(c.getCanonicalName()));
        });
    }

    @Override
    public void close() throws CatalogException {
        // close any clients and caches that might be open
        kafkaClient.close();
        registryClient.close();
    }

    @Override
    public List<String> listDatabases() throws CatalogException {
        return Arrays.asList(SOURCE_DATABASE, PROFILE_DATABASE);
    }

    @Override
    public CatalogDatabase getDatabase(String databaseName) throws DatabaseNotExistException, CatalogException {
        if (databaseName.equals(SOURCE_DATABASE)) {
            return new CatalogDatabaseImpl(Collections.emptyMap(), "Sources available for profiling");
        }
        if (databaseName.equals(PROFILE_DATABASE)) {
            return new CatalogDatabaseImpl(Collections.emptyMap(), "Profiles available");
        }
        throw new DatabaseNotExistException(CATALOG_NAME, databaseName);
    }

    @Override
    public boolean databaseExists(String databaseName) throws CatalogException {
        return listDatabases().contains(databaseName);
    }

    @Override
    public List<String> listTables(String database) throws DatabaseNotExistException, CatalogException {
        // list all the topics, which have an appropriate schema with them
        if (database.equals(PROFILE_DATABASE))
            return Arrays.asList("profiles");

        if (database.equals(SOURCE_DATABASE))
            try {
                return new ArrayList<String>(getSourceTables());
            } catch (Exception e) {
                throw new CatalogException("Error listing source topics", e);
            }

        throw new DatabaseNotExistException(CATALOG_NAME, database);
    }

    @Override
    public List<String> listViews(String databaseName) throws DatabaseNotExistException, CatalogException {
        if (!databaseExists(databaseName))
            throw new DatabaseNotExistException(CATALOG_NAME, databaseName);

        return Collections.emptyList();
    }

    @Override
    public CatalogBaseTable getTable(ObjectPath objectPath) throws TableNotExistException, CatalogException {
        if (objectPath.getDatabaseName().equals(PROFILE_DATABASE)) {
            if (!objectPath.getObjectName().equals("profiles"))
                throw new TableNotExistException(CATALOG_NAME, objectPath);

            // return the schema of the profile results table
            String topic = objectPath.getObjectName();

            // ensure the topic exists, and that a compatible schema is associated with it

            Kafka kafkaConnector = new Kafka().properties(readKafkaProperties(this.properties, "profiler-catalog", true)).topic(topic);
            try {
                TableSchema schema = schemaForTopicFromRegistry(topic);

                return new CatalogTableBuilder(kafkaConnector, schema)
                        .withComment("Profiler source table from " + topic)
                        .withFormat(new Avro())
                        .build();

            } catch (SchemaNotFoundException e) {
                throw new TableNotExistException(CATALOG_NAME, objectPath, e);
            }

        }

        if (objectPath.getDatabaseName().equals(SOURCE_DATABASE)) {
            String table = objectPath.getObjectName();

        }
        throw new TableNotExistException(CATALOG_NAME, objectPath);
    }

    @Override
    public boolean tableExists(ObjectPath objectPath) throws CatalogException {
        if (objectPath.getDatabaseName().equals(PROFILE_DATABASE) && objectPath.getObjectName() != "profiles")
            return false;

        if (objectPath.getDatabaseName().equals(SOURCE_DATABASE)) {
            try {
                return listTables(SOURCE_DATABASE).contains(objectPath.getObjectName());
            } catch (DatabaseNotExistException e) {
                // should never occur
                throw new CatalogException("Source Database somehow doesn't exist", e);
            }
        }
        return false;
    }

    @Override
    public List<CatalogPartitionSpec> listPartitions(ObjectPath objectPath) throws TableNotExistException, TableNotPartitionedException, CatalogException {
        return Collections.emptyList();
    }

    @Override
    public List<CatalogPartitionSpec> listPartitions(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws TableNotExistException, TableNotPartitionedException, CatalogException {
        return Collections.emptyList();
    }

    @Override
    public List<CatalogPartitionSpec> listPartitionsByFilter(ObjectPath objectPath, List<Expression> list) throws TableNotExistException, TableNotPartitionedException, CatalogException {
        return Collections.emptyList();
    }

    @Override
    public boolean partitionExists(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws CatalogException {
        return false;
    }

    @Override
    public List<String> listFunctions(String databaseName) throws DatabaseNotExistException, CatalogException {
        if (!databaseExists(databaseName))
            throw new DatabaseNotExistException(CATALOG_NAME, databaseName);
        return new ArrayList<>(functions.keySet());
    }

    @Override
    public CatalogFunction getFunction(ObjectPath objectPath) throws FunctionNotExistException, CatalogException {
        if (!functions.containsKey(objectPath.getObjectName()))
            throw new FunctionNotExistException(CATALOG_NAME, objectPath);
        return functions.get(objectPath.getObjectName());
    }

    @Override
    public boolean functionExists(ObjectPath objectPath) throws CatalogException {
        return databaseExists(objectPath.getDatabaseName()) && functions.containsKey(objectPath.getObjectName());
    }

    /**
     * TODO - we may be able to collect some average row size stats over time
     *
     * @param objectPath
     * @return
     * @throws TableNotExistException
     * @throws CatalogException
     */
    @Override
    public CatalogTableStatistics getTableStatistics(ObjectPath objectPath) throws TableNotExistException, CatalogException {
        return CatalogTableStatistics.UNKNOWN;
    }

    @Override
    public CatalogColumnStatistics getTableColumnStatistics(ObjectPath objectPath) throws TableNotExistException, CatalogException {
        return CatalogColumnStatistics.UNKNOWN;
    }

    @Override
    public CatalogTableStatistics getPartitionStatistics(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws PartitionNotExistException, CatalogException {
        return CatalogTableStatistics.UNKNOWN;
    }

    @Override
    public CatalogColumnStatistics getPartitionColumnStatistics(ObjectPath objectPath, CatalogPartitionSpec catalogPartitionSpec) throws PartitionNotExistException, CatalogException {
        return CatalogColumnStatistics.UNKNOWN;
    }
}
