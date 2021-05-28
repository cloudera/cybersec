package com.cloudera.cyber.enrichment.load;

import com.cloudera.cyber.commands.CommandType;
import com.cloudera.cyber.commands.EnrichmentCommand;
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

public abstract class BatchEnrichmentLoaderCSV extends BatchEnrichmentLoader {

    protected static final String ENRICHMENT_SOURCE_FILE = "enrichment.source.file";
    protected static final String ENRICHMENT_SKIP_FIRST_LINE = "enrichment.source.skipfirstline";
    protected static final String ENRICHMENT_FIELDS = "enrichment.source.fieldnames";
    protected static final String ENRICHMENT_KEY_FIELD_NAME = "enrichment.source.keyfieldname";
    protected static final String ENRICHMENT_COLUMN_FAMILY = "enrichment.type";

    @Override
    protected DataStream<EnrichmentCommand> createEnrichmentSource(StreamExecutionEnvironment env, ParameterTool params) {
        String path =  params.getRequired(ENRICHMENT_SOURCE_FILE);
        boolean ignoreFirstLine = params.getBoolean(ENRICHMENT_SKIP_FIRST_LINE, false);
        List<String> fieldNames = Arrays.asList(params.get(ENRICHMENT_FIELDS).split(",", -1));
        String keyFieldName = params.getRequired(ENRICHMENT_KEY_FIELD_NAME);
        String enrichmentType = params.getRequired(ENRICHMENT_COLUMN_FAMILY);
        List<String> valueFieldNames = fieldNames.stream().filter(f -> !f.isEmpty() && !f.equals(keyFieldName)).collect(Collectors.toList());

        Preconditions.checkArgument(valueFieldNames.size() >=1, "Field names must contain at least onve value associated with the key fields");
        Preconditions.checkArgument(fieldNames.stream().filter(f -> f.equals(keyFieldName)).count() == 1, ENRICHMENT_KEY_FIELD_NAME + " is not included in " + ENRICHMENT_FIELDS);

        // create a CSV table source from the specified file
        CsvTableSource.Builder builder = CsvTableSource.builder().path(path);
        if (ignoreFirstLine) {
            builder.ignoreFirstLine();
        }

        // add a field for each column, any ignored column is named ignore_<number>
        IntStream.range(0, fieldNames.size()).
                mapToObj(index -> !fieldNames.get(index).isEmpty() ? fieldNames.get(index) : String.format("ignore_%d", index)).
                forEach(f -> builder.field(f, DataTypes.STRING()));

        CsvTableSource tableSource = builder.build();

        EnvironmentSettings settings = EnvironmentSettings.newInstance().useBlinkPlanner().inStreamingMode().build();
        StreamTableEnvironment streamTableEnv = StreamTableEnvironment.create(env, settings);


        String fieldSelection = Stream.concat(Stream.of(keyFieldName), valueFieldNames.stream()).collect(Collectors.joining(","));
        Table enrichmentTable = streamTableEnv.fromTableSource(tableSource).select(fieldSelection);

        DataStream<Row> enrichmentStream = streamTableEnv.toAppendStream(enrichmentTable, Row.class);
        MapRowToEnrichmentCommand csvToEnrichmentCommand = new MapRowToEnrichmentCommand(enrichmentType, CommandType.ADD, valueFieldNames);
        return enrichmentStream.map(csvToEnrichmentCommand);
    }
}
