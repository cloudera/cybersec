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

package com.cloudera.cyber.generator;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.Charset;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.extern.java.Log;
import org.apache.avro.Schema;
import org.apache.avro.Schema.Parser;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.commons.math3.util.Pair;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.source.ParallelSourceFunction;

@Log
public class FreemarkerTemplateSource implements ParallelSourceFunction<Tuple2<String, byte[]>> {

    private final EnumeratedDistribution<GenerationSource> files;
    private volatile boolean isRunning = true;
    private static final RandomGenerators utils = new RandomGenerators();

    // total number of events to generate
    private final long maxRecords;
    private long count = 0;

    private final int eps;
    private final List<GenerationSource> generationSources = new ArrayList<>();
    private final String templateBaseDir;

    private static class JsonStringToAvro implements Function<String, byte[]> {

        private final Schema avroSchema;

        JsonStringToAvro(String schemaString) {
            Parser avroSchemaParser = new Parser();
            this.avroSchema = avroSchemaParser.parse(schemaString);
        }

        @Override
        public byte[] apply(String s) {
            return Utils.jsonDecodeToAvroByteArray(s, avroSchema);
        }
    }

    private static class TextToBytes implements Function<String, byte[]> {

        @Override
        public byte[] apply(String s) {
            return s.getBytes(Charset.defaultCharset());
        }
    }


    public FreemarkerTemplateSource(GeneratorConfig generatorConfig, long maxRecords, int eps) {
        // normalise weights
        List<GenerationSource> files = generatorConfig.getGenerationSources();
        Double total = files.stream().mapToDouble(GenerationSource::getWeight).sum();
        List<Pair<GenerationSource, Double>> weights = files.stream()
              .map(e -> Pair.create(e, e.getWeight() / total))
              .collect(Collectors.toList());

        // create a reverse sorted version of the weights
        this.files = new EnumeratedDistribution<>(weights);

        this.maxRecords = maxRecords;

        this.eps = eps;

        this.generationSources.addAll(files);
        this.templateBaseDir = generatorConfig.getBaseDirectory();

    }

    @Override
    public void run(SourceContext<Tuple2<String, byte[]>> sourceContext) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);

        TemplateLoader templateLoader = new ClassTemplateLoader(Thread.currentThread().getContextClassLoader(), "");
        if (templateBaseDir != null) {
            FlinkFileTemplateLoader flinkFileLoader = new FlinkFileTemplateLoader(templateBaseDir);
            templateLoader = new MultiTemplateLoader(new TemplateLoader[] {flinkFileLoader, templateLoader});
        }

        cfg.setTemplateLoader(templateLoader);
        cfg.setCacheStorage(new freemarker.cache.MruCacheStorage(50, 50));
        cfg.setTemplateUpdateDelayMilliseconds(3600 * 24 * 1000);

        Map<String, Function<String, byte[]>> topicOutputConverter = new HashMap<>();

        for (GenerationSource generationSource : generationSources) {
            if (generationSource.getOutputAvroSchema() != null) {
                topicOutputConverter.put(generationSource.getTopic(),
                      new JsonStringToAvro(generationSource.getOutputAvroSchema()));
            } else {
                topicOutputConverter.put(generationSource.getTopic(), new TextToBytes());
            }
            generationSource.readScenarioFile(templateBaseDir);
        }

        int ms = 0;
        int ns = 0;
        if (eps != 0) {
            if (eps > 1000) {
                // send a batch per sleep
                ns = 1000000 / eps;
            } else {
                ms = 1000 / eps;
            }
        }

        Instant startTime = Instant.now();

        while (isRunning && (this.maxRecords == -1 || this.count < this.maxRecords)) {
            this.count++;


            // figure out which template we're using, i.e. weighted by the files map
            GenerationSource file = files.sample();
            Template temp = cfg.getTemplate(file.getFile());
            Writer out = new StringWriter();

            SyntheticEntry entry = SyntheticEntry.builder().ts(
                        Instant.now().toEpochMilli())
                  .utils(utils)
                  .params(file.getRandomParameters())
                  .build();

            temp.process(entry, out);

            if (count % 100 == 0) {
                Instant endTime = Instant.now();
                log.info(String.format("Produced %d records from template %s to topic %s from: %s to: %s", count,
                      file.getFile(), file.getTopic(), startTime, endTime));
                startTime = endTime;
            }

            sourceContext.collect(
                  Tuple2.of(file.getTopic(), topicOutputConverter.get(file.getTopic()).apply(out.toString())));
            if (eps > 0) {
                Thread.sleep(ms, ns);
            }
        }

    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
