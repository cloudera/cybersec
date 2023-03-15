/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentFieldsConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentStorageConfig;
import com.cloudera.cyber.enrichment.hbase.config.EnrichmentsConfig;
import com.google.common.collect.Lists;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.eventtime.WatermarkStrategy;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.api.java.typeutils.ListTypeInfo;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.connector.file.src.FileSource;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.formats.csv.CsvReaderFormat;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.core.type.TypeReference;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.module.SimpleModule;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvSchema;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;
import org.apache.flink.util.Preconditions;

import java.io.IOException;
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
    protected void load(StreamExecutionEnvironment env, ParameterTool params) throws IOException {
        String path =  params.getRequired(ENRICHMENT_SOURCE_FILE);
        boolean ignoreFirstLine = params.getBoolean(ENRICHMENT_SKIP_FIRST_LINE, false);
        List<String> columnNames = Arrays.asList(params.getRequired(ENRICHMENT_COLUMNS).split(",", -1));
        String enrichmentType = params.getRequired(ENRICHMENT_TYPE);
        EnrichmentsConfig enrichmentsConfig = getEnrichmentsConfig(params, enrichmentType, columnNames);
        EnrichmentConfig enrichmentConfig = enrichmentsConfig.getEnrichmentConfigs().get(enrichmentType);
        List<String> keyFields = enrichmentConfig.getFieldMapping().getKeyFields();
        List<String> configuredValueFields = enrichmentConfig.getFieldMapping().getValueFields();
        List<String> valueFieldNames = columnNames.stream().filter(f -> !f.isEmpty() && !keyFields.contains(f) && (configuredValueFields == null || configuredValueFields.contains(f))).collect(Collectors.toList());

        // create a CSV file source from the specified file
        CsvMapper mapper = new CsvMapper();
        CsvSchema.Builder schemaBuilder = CsvSchema.builder();

        // add a field for each column, any ignored column is named ignore_<number>
        IntStream.range(0, columnNames.size()).
                mapToObj(index -> !columnNames.get(index).isEmpty() ? columnNames.get(index) : String.format("ignore_%d", index)).
                forEach(f -> schemaBuilder.addColumn(f, CsvSchema.ColumnType.STRING));

        CsvSchema schema = schemaBuilder.build().withSkipFirstDataRow(ignoreFirstLine).withLineSeparator("\n");

        SimpleModule module = new SimpleModule();
        module.addDeserializer(EnrichmentCommand.class, new CsvToEnrichmentCommandDeserializer(enrichmentType, CommandType.ADD, keyFields, ":", valueFieldNames));
        mapper.registerModule(module);

        CsvReaderFormat<EnrichmentCommand> csvFormat =
                CsvReaderFormat.forSchema(mapper, schema, TypeInformation.of(EnrichmentCommand.class));

        Path csvPath = new Path(path);
        FileSystem fs = csvPath.getFileSystem();
        if (!fs.exists(csvPath)) {
            log.error("Could not load CSV input file '{}'", path);
            return;
        }

        FileSource<EnrichmentCommand> source =
                FileSource.forRecordStreamFormat(csvFormat, csvPath).build();

        DataStream<EnrichmentCommand> csvEnrichment = env.fromSource(source, WatermarkStrategy.noWatermarks(), "Enrichment CSV");

       // MapRowToEnrichmentCommand csvToEnrichmentCommand = new MapRowToEnrichmentCommand(enrichmentType, CommandType.ADD, valueFieldMapping, keyFieldIndices,
       //         enrichmentConfig.getFieldMapping().getKeyDelimiter());
        writeResults(params, enrichmentsConfig, enrichmentType, csvEnrichment, env);
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
