package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.table.api.DataTypes;
import org.apache.flink.table.api.EnvironmentSettings;
import org.apache.flink.table.api.Table;
import org.apache.flink.table.api.bridge.java.StreamTableEnvironment;
import org.apache.flink.table.sources.CsvTableSource;
import org.apache.flink.types.Row;
import org.apache.flink.util.Preconditions;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageFormat.HBASE_SIMPLE;
import static com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig.DEFAULT_ENRICHMENT_STORAGE_NAME;

@Slf4j
public abstract class BatchEnrichmentLoaderCSV extends BatchEnrichmentLoader {

    protected static final String ENRICHMENT_SOURCE_FILE = "enrichment.source.file";
    protected static final String ENRICHMENT_SKIP_FIRST_LINE = "enrichment.source.skipfirstline";
    protected static final String ENRICHMENT_COLUMNS = "enrichment.source.columns";
    protected static final String ENRICHMENT_KEY_FIELD_NAME = "enrichment.source.keyfieldname";
    protected static final String ENRICHMENT_TYPE = "enrichment.type";
    protected static final String PARAMS_ENRICHMENT_FILE = "enrichment.config.file";
    protected static final String PARAMS_ENRICHMENTS_TABLE = "enrichments.table";

    @Override
    protected void load(StreamExecutionEnvironment env, ParameterTool params) {
        String path =  params.getRequired(ENRICHMENT_SOURCE_FILE);
        boolean ignoreFirstLine = params.getBoolean(ENRICHMENT_SKIP_FIRST_LINE, false);
        List<String> columnNames = Arrays.asList(params.getRequired(ENRICHMENT_COLUMNS).split(",", -1));
        String enrichmentType = params.getRequired(ENRICHMENT_TYPE);
        EnrichmentsConfig enrichmentsConfig = getEnrichmentsConfig(params, enrichmentType, columnNames);
        EnrichmentConfig enrichmentConfig = enrichmentsConfig.getEnrichmentConfigs().get(enrichmentType);
        List<String> keyFields = enrichmentConfig.getFieldMapping().getKeyFields();
        List<String> configuredValueFields = enrichmentConfig.getFieldMapping().getValueFields();

        List<String> valueFieldNames = columnNames.stream().filter(f -> !f.isEmpty() && !keyFields.contains(f) && (configuredValueFields == null || configuredValueFields.contains(f))).collect(Collectors.toList());

        // create a CSV table source from the specified file
        CsvTableSource.Builder builder = CsvTableSource.builder().path(path);
        if (ignoreFirstLine) {
            builder.ignoreFirstLine();
        }

        // add a field for each column, any ignored column is named ignore_<number>
        IntStream.range(0, columnNames.size()).
                mapToObj(index -> !columnNames.get(index).isEmpty() ? columnNames.get(index) : String.format("ignore_%d", index)).
                forEach(f -> builder.field(f, DataTypes.STRING()));

        CsvTableSource tableSource = builder.build();

        EnvironmentSettings settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment streamTableEnv = StreamTableEnvironment.create(env, settings);


        String fieldSelection = Stream.concat(enrichmentConfig.getFieldMapping().getKeyFields().stream(), valueFieldNames.stream()).collect(Collectors.joining(","));
        Table enrichmentTable = streamTableEnv.fromTableSource(tableSource).select(fieldSelection);

        DataStream<Row> enrichmentStream = streamTableEnv.toAppendStream(enrichmentTable, Row.class);
        MapRowToEnrichmentCommand csvToEnrichmentCommand = new MapRowToEnrichmentCommand(enrichmentType, CommandType.ADD, valueFieldNames, enrichmentConfig.getFieldMapping().getKeyDelimiter(), enrichmentConfig.getFieldMapping().getKeyFields().size() );
        writeResults(params, enrichmentsConfig, enrichmentType, enrichmentStream.map(csvToEnrichmentCommand), env);
    }

    private EnrichmentsConfig getEnrichmentsConfig(ParameterTool params, String enrichmentType, List<String> columnNames) {
        String enrichmentConfigFile = params.get(PARAMS_ENRICHMENT_FILE);
        String keyFieldName = params.get(ENRICHMENT_KEY_FIELD_NAME);
        EnrichmentsConfig enrichmentsConfig;
        EnrichmentConfig enrichmentConfig;

        if (enrichmentConfigFile != null) {
            enrichmentsConfig = EnrichmentsConfig.load(enrichmentConfigFile);
            enrichmentConfig = enrichmentsConfig.getEnrichmentConfigs().get(enrichmentType);
            Preconditions.checkNotNull(enrichmentConfig, String.format("No configuration for enrichmentType '%s' found in enrichments config file '%s'", enrichmentType, enrichmentConfigFile));

            Preconditions.checkState(keyFieldName == null,
                    String.format("Key field name should not be specified when using %s configuration.", PARAMS_ENRICHMENT_FILE));

            List<String> valueFields = enrichmentConfig.getFieldMapping().getValueFields();
            if (valueFields != null) {
                List<String> valueFieldsInColumns = columnNames.stream().filter(valueFields::contains).collect(Collectors.toList());
                Preconditions.checkState(!valueFieldsInColumns.isEmpty(),
                        String.format("Columns specified in csv '%s' do not contain any value fields '%s'", String.join(",", columnNames), String.join(",", valueFields)));
            }
            log.info("Using enrichment config file {}", enrichmentConfig);
        } else {
            Preconditions.checkNotNull(keyFieldName, String.format("Missing key field configuration %s", ENRICHMENT_KEY_FIELD_NAME));
            String enrichmentsTable = params.getRequired(PARAMS_ENRICHMENTS_TABLE);
            enrichmentsConfig = new EnrichmentsConfig();
            enrichmentsConfig.getStorageConfigs().put(DEFAULT_ENRICHMENT_STORAGE_NAME, new EnrichmentStorageConfig(HBASE_SIMPLE, enrichmentsTable, null));
            EnrichmentFieldsConfig fieldsConfig = new EnrichmentFieldsConfig(Lists.newArrayList(keyFieldName), null, null, null);
            enrichmentConfig =  new EnrichmentConfig(DEFAULT_ENRICHMENT_STORAGE_NAME, fieldsConfig);
            enrichmentsConfig.getEnrichmentConfigs().put(enrichmentType, enrichmentConfig);
            log.info("Using default config {}", enrichmentConfig);
        }

        List<String> keyFields = enrichmentConfig.getFieldMapping().getKeyFields();
        List<String> missingKeyFields = keyFields.stream().filter(kf -> !columnNames.contains(kf)).collect(Collectors.toList());
        Preconditions.checkState(missingKeyFields.isEmpty(),
                String.format("Columns '%s' are missing key field(s) '%s'", String.join(",", columnNames), String.join(", ", missingKeyFields)));

        return enrichmentsConfig;
    }
}
