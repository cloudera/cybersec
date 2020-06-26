package com.cloudera.cyber.test.generator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import org.apache.commons.math3.util.Pair;
import org.apache.commons.math3.distribution.EnumeratedDistribution;
import org.apache.flink.api.java.tuple.Tuple2;
import org.apache.flink.streaming.api.functions.source.ParallelSourceFunction;

import java.io.File;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class FreemarkerTemplateSource implements ParallelSourceFunction<Tuple2<String,String>> {

    private final EnumeratedDistribution<GenerationSource> files;
    private volatile boolean isRunning = true;
    private static final RandomGenerators utils = new RandomGenerators();

    public FreemarkerTemplateSource(Map<GenerationSource, Double> files) {
        // normalise weights
        Double total = files.entrySet().stream().collect(Collectors.summingDouble(v -> v.getValue()));
        List<Pair<GenerationSource, Double>> weights = files.entrySet().stream()
                .map(e -> Pair.create(e.getKey(), e.getValue() / total))
                .collect(Collectors.toList());

        // create a reverse sorted version of the weights
        this.files = new EnumeratedDistribution<GenerationSource>(weights);
    }

    @Override
    public void run(SourceContext<Tuple2<String,String>> sourceContext) throws Exception {
        Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);

        URL url = ClassLoader.getSystemResource("");
        File templateLocation = new File(url.toURI());
        cfg.setDirectoryForTemplateLoading(templateLocation);
        cfg.setCacheStorage(new freemarker.cache.MruCacheStorage(50,50));
        cfg.setTemplateUpdateDelayMilliseconds(3600*24*1000);

        while (isRunning) {
            SyntheticEntry entry = SyntheticEntry.builder().ts(
                    LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli())
                    .utils(utils)
                    .build();

            // figure out which template we're using, i.e. weighted by the files map
            GenerationSource file = files.sample();
            Template temp = cfg.getTemplate(file.getFile());
            Writer out = new StringWriter();
            temp.process(entry, out);

            sourceContext.collect(Tuple2.of(file.getTopic(), out.toString()));
        }
    }

    @Override
    public void cancel() {
        isRunning = false;
    }
}
