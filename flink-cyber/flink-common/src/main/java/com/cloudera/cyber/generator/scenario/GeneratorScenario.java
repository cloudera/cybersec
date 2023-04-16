package com.cloudera.cyber.generator.scenario;

import com.cloudera.cyber.generator.RandomGenerators;
import com.google.common.base.Preconditions;
import org.apache.commons.io.IOUtils;
import org.apache.flink.core.fs.Path;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.MappingIterator;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.databind.ObjectReader;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvMapper;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvParser;
import org.apache.flink.shaded.jackson2.com.fasterxml.jackson.dataformat.csv.CsvSchema;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.Charset;
import java.util.List;
import java.util.Map;


public class GeneratorScenario {
    protected final static String NOT_ENOUGH_LINES_ERROR = "Scenario CSV file must contain a header and at least one value line";
    private final List<String> lines;
    private final ObjectReader csvReader;

    public static GeneratorScenario load(String csvFile) throws IOException {

        GeneratorScenario scenario;
        Path csvPath = new Path(csvFile);
        try (InputStream csvStream = csvPath.getFileSystem().open(csvPath)) {
            List<String> lines = IOUtils.readLines(csvStream, Charset.defaultCharset());
            Preconditions.checkState(lines.size() >= 2, NOT_ENOUGH_LINES_ERROR);
            scenario = new GeneratorScenario(lines);
         }

        return scenario;
    }

    private GeneratorScenario(List<String> lines) throws IOException {
        this.lines = lines;
        List<String> header = readCsvHeader(lines.get(0));
        this.csvReader = createCsvReader(header);
    }

    public Map<String, String> randomParameters() throws IOException {
        int randomIndex = RandomGenerators.randomInt(1,lines.size() - 1);
        return csvReader.readValue(lines.get(randomIndex));
    }

    private ObjectReader createCsvReader(List<String> header) {
        CsvSchema.Builder builder = CsvSchema.builder();
        header.forEach(builder::addColumn);
        final CsvMapper mapper = new CsvMapper();
        return mapper
                .readerForMapOf(String.class)
                .with(builder.build());
    }

    private List<String> readCsvHeader(String header) throws IOException {
        final CsvMapper mapper = new CsvMapper();
        MappingIterator<List<String>> it = mapper
                .readerForListOf(String.class)
                .with(CsvParser.Feature.WRAP_AS_ARRAY)
                .readValues(header);

        return it.nextValue();
    }
}
