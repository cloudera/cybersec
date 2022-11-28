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

import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.IntStream;

public class ThreatGenerator {
    private List<String> lines;
    private Configuration cfg;

    public ThreatGenerator() {
        cfg = new Configuration(Configuration.VERSION_2_3_30);
        cfg.setClassLoaderForTemplateLoading(Thread.currentThread().getContextClassLoader(), "");
        cfg.setCacheStorage(new freemarker.cache.MruCacheStorage(50, 50));
        cfg.setTemplateUpdateDelayMilliseconds(3600 * 24 * 1000);
        try {
            lines = IOUtils.readLines(getClass().getResourceAsStream("/threats/threatq.json"));
            StringTemplateLoader stringLoader = new StringTemplateLoader();
            IntStream.range(0, lines.size()).forEach(i -> {
                stringLoader.putTemplate(String.valueOf(i), lines.get(i));
            });
            cfg.setTemplateLoader(stringLoader);
        } catch (IOException ioException) {
            ioException.printStackTrace();
        }
    }

    private static final RandomGenerators utils = new RandomGenerators();

    /**
     * Choose a random threat template, insert the IP address and return the string
     * <p>
     * random line from the threats resource, treat as freemarker and expose
     *
     * @param ip
     * @return
     */
    public String generateThreat(String ip) throws IOException, TemplateException {
        String template = String.valueOf(ThreadLocalRandom.current().nextInt(lines.size() - 1));

        SyntheticThreatEntry entry = SyntheticThreatEntry.builder().ts(
                LocalDateTime.now().toInstant(ZoneOffset.UTC).toEpochMilli())
                .utils(utils)
                .ip(ip)
                .build();

        // figure out which template we're using, i.e. weighted by the files map
        Template temp = cfg.getTemplate(template);
        Writer out = new StringWriter();
        temp.process(entry, out);

        return out.toString();
    }
}
