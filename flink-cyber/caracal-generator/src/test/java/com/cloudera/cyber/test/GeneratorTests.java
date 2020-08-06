package com.cloudera.cyber.test;

import com.cloudera.cyber.test.generator.FreemarkerImmediateGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateException;
import org.hamcrest.MatcherAssert;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.collection.IsMapContaining.hasEntry;
import static org.hamcrest.collection.IsMapContaining.hasKey;
import static org.hamcrest.text.MatchesPattern.matchesPattern;

public class GeneratorTests {

    @Test
    public void testNetflowSimple() throws IOException, URISyntaxException, TemplateException {

        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();

        String result = generator.generateEntry("Netflow/netflow_sample_1.json");

        assertThat("Result is not null", result, notNullValue());
    }

    @Test
    public void testNetflowHttps() throws IOException, URISyntaxException, TemplateException {

        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();

        String result = generator.generateEntry("Netflow/netflow_sample_2.json");

        assertThat("Result is not null", result, notNullValue());

        ObjectMapper mapper = new ObjectMapper();
        HashMap output = mapper.readValue(result, HashMap.class);

        assertThat("port", (Integer) output.get("dst_port"), equalTo(443));
        assertThat("dst_bytes is not null", result, notNullValue());
    }

    @Test
    public void testNetflowHttp() throws IOException, URISyntaxException, TemplateException {
        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();

        String result = generator.generateEntry("Netflow/netflow_sample_3.json");

        assertThat("Result is not null", result, notNullValue());

        ObjectMapper mapper = new ObjectMapper();
        HashMap output = mapper.readValue(result, HashMap.class);

        assertThat("port", (Integer) output.get("dst_port"), equalTo(80));
        assertThat("dst_bytes is not null", result, notNullValue());
    }


    @Test(timeout = 1000)
    public void testBulkProduction10000eps() throws IOException, URISyntaxException, TemplateException {
        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();

        for (int i = 0; i < 10000; i++) {
            String result = generator.generateEntry("Netflow/netflow_sample_3.json");
        }
    }


    private HashMap testFile(String file) throws IOException, URISyntaxException, TemplateException {
        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();
        String result = generator.generateEntry(file);
        Assert.assertNotNull(result);
        ObjectMapper mapper = new ObjectMapper();
        HashMap output = mapper.readValue(result, HashMap.class);
        Assert.assertNotNull(output);
        return output;
    }

    @Test
    public void testAllDNS() throws IOException, URISyntaxException, TemplateException {
        for (int i = 1; i < 4; i++) {
            testFile(String.format("DPI_Logs/Metadata_Module/DNS/dns_sample_%d.json", i));
        }
    }

    @Test
    public void testRadius() throws TemplateException, IOException, URISyntaxException {
        testFile("DPI_Logs/Metadata_Module/RADIUS/radius_sample_1.json");
    }

    @Test
    public void testSMTP() throws TemplateException, IOException, URISyntaxException {
        HashMap<String, Object> hashMap = testFile("DPI_Logs/Metadata_Module/SMTP/smtp_sample_1.json");
        List<Map<String, Object>> email = (List<Map<String, Object>>) ((Map<String, Object>) hashMap.get("smtp-stream")).get("smtp.email");

        assertThat("SMTP date exists", email.get(0), hasKey("smtp.date"));
        assertThat("SMTP date good", (String) email.get(0).get("smtp.date"), matchesPattern("^[0-9]*$"));
    }
}
