package com.cloudera.cyber.ondemand.config;

import com.cloudera.cyber.flink.Utils;
import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.hive.HiveJob;
import com.cloudera.cyber.indexing.hive.tableapi.IndexTableMappingConfiguration;
import com.cloudera.cyber.indexing.hive.tableapi.TableApiJobFactory;
import com.cloudera.cyber.indexing.hive.tableapi.impl.TableApiIcebergJob;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.util.Preconditions;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public record OndemandEnrichmentConfiguration(LocalDateTime startTime, LocalDateTime endTime,
                                              List<String> sourcesToEnrich,
                                              IndexTableMappingConfiguration indexTableMappingConfiguration,
                                              String destinationDatabase) {

    private static final String DESTINATION_DATABASE_PARAM_KEY = "destination.hive.dbname";
    public static final String SOURCES_PARAM_KEY = "sources";

    public static OndemandEnrichmentConfiguration readConfiguration(ParameterTool params) throws IOException {
        // read the configurations used for the triage and index jobs - ondemand params refer to other configs by file path
        ParameterTool triageParams = Utils.getParamToolsFromProperties(new String[]{params.get("triage.config")});
        ParameterTool indexParams = Utils.getParamToolsFromProperties(new String[]{params.get("index.config")});
        String indexConnector = HiveJob.getIndexOutputConnectorType(indexParams);
        Preconditions.checkArgument(indexConnector.equals(TableApiJobFactory.ICEBERG_INDEX_JOB_CONNECTOR),
                String.format("Ondemand enrichment does not support connector %s.  Only iceberg is supported.", indexConnector));

        IndexTableMappingConfiguration indexTableMappingConfiguration = new IndexTableMappingConfiguration(TableApiIcebergJob.BASE_TABLE_JSON);
        indexTableMappingConfiguration.readConfiguration(indexParams, triageParams);

        // get the start and end timestamp strings and validate format is correct
        LocalDateTime startTime = TimestampConfig.getConfiguration(params, "start.timestamp");
        LocalDateTime endTime = TimestampConfig.getConfiguration(params, "end.timestamp");

        List<String> sourcesToEnrich = getSourcesToEnrich(params, indexTableMappingConfiguration);

        String destinationDatabase = getDestinationDatabase(params, indexParams);

        return new OndemandEnrichmentConfiguration(startTime, endTime, sourcesToEnrich, indexTableMappingConfiguration, destinationDatabase);
    }

    private static String getDestinationDatabase(ParameterTool params, ParameterTool indexParams) {
        String destinationDatabase = params.getRequired(DESTINATION_DATABASE_PARAM_KEY);

        Preconditions.checkArgument(!destinationDatabase.equalsIgnoreCase(TableApiIcebergJob.getDatabase(indexParams)),
                String.format("Configuration %s and source iceberg database are both set to %s.  To avoid overwriting source index tables, the ondemand databases result must be different.",
                        DESTINATION_DATABASE_PARAM_KEY, destinationDatabase));
        return destinationDatabase;
    }

    private static List<String> getSourcesToEnrich(ParameterTool params, IndexTableMappingConfiguration indexTableMappingConfiguration) {
        List<String> sourcesToEnrich = Stream.of(params.getRequired(SOURCES_PARAM_KEY).split(",")).distinct().toList();
        Map<String, MappingDto> unenrichedTopicMapping = indexTableMappingConfiguration.getUnenrichedTopicMapping();

        List<String> unmappedSources = sourcesToEnrich.stream().filter(sourceToEnrich -> !(unenrichedTopicMapping.containsKey(sourceToEnrich))).toList();
        Preconditions.checkArgument(!unmappedSources.isEmpty(),
                String.format("The %s configuration specifies '%s' as sources for ondemand enrichment but there are no mappings defined in the index configuration.",
                        SOURCES_PARAM_KEY, String.join(", ", unmappedSources)));
        return sourcesToEnrich;
    }
}
