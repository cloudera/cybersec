package com.cloudera.cyber.test.generator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneOffset;

public class FreemarkerImmediateGenerator {

    Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
    private static final RandomGenerators utils = new RandomGenerators();

    public void configureTemplate() throws URISyntaxException, IOException {
        URL url = ClassLoader.getSystemResource("");
        File templateLocation = new File(url.toURI());
        cfg.setDirectoryForTemplateLoading(templateLocation);
        cfg.setCacheStorage(new freemarker.cache.MruCacheStorage(50,150));
        cfg.setTemplateUpdateDelayMilliseconds(3600*24*1000);
    }

    public String generateEntry(String template) throws IOException, TemplateException {
        SyntheticEntry entry = SyntheticEntry.builder().ts(
                LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli())
                .utils(utils)
                .build();
        Template temp = cfg.getTemplate(template);

        Writer out = new StringWriter();
        temp.process(entry, out);
        return out.toString();
    }
}
