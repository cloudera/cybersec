package com.cloudera.cyber.generator;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

public class FreemarkerImmediateGenerator {

    Configuration cfg = new Configuration(Configuration.VERSION_2_3_30);
    private static final RandomGenerators utils = new RandomGenerators();

    public FreemarkerImmediateGenerator()  {
        cfg.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "");
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

    public <K,V> String replaceByFile(String pathToFile, Map<K,V> params) throws IOException, TemplateException {
        Template template = cfg.getTemplate(pathToFile);
        Writer out = new StringWriter();
        template.process(params, out);
        return out.toString();
    }

    public <K,V> String replceByTemplate(String templateString, Map<K,V> params) throws IOException, TemplateException {
        Template template = new Template("templateName", templateString, cfg);
        Writer out = new StringWriter();
        template.process(params, out);
        return out.toString();
    }
}