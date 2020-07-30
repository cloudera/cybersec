package com.cloudera.cyber.test;

import com.cloudera.cyber.test.generator.FreemarkerImmediateGenerator;
import com.fasterxml.jackson.databind.ObjectMapper;
import freemarker.template.TemplateException;
import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.HashMap;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class GeneratorTests {

    @Test
    public void testNetflowSimple() throws IOException, URISyntaxException, TemplateException {

        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();

        String result = generator.generateEntry("Netflow/netflow_sample_1.json");

        assertNotNull(result);
    }

    @Test
    public void testNetflowHttps() throws IOException, URISyntaxException, TemplateException {

        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();

        String result = generator.generateEntry("Netflow/netflow_sample_2.json");

        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        HashMap output = mapper.readValue(result, HashMap.class);

        assertEquals(Integer.valueOf(443), (Integer) output.get("dst_port"));
        assertNotNull(output.get("dst_bytes"));
    }

    @Test
    public void testNetflowHttp() throws IOException, URISyntaxException, TemplateException {
        FreemarkerImmediateGenerator generator = new FreemarkerImmediateGenerator();
        generator.configureTemplate();

        String result = generator.generateEntry("Netflow/netflow_sample_3.json");

        assertNotNull(result);

        ObjectMapper mapper = new ObjectMapper();
        HashMap output = mapper.readValue(result, HashMap.class);

        assertEquals(Integer.valueOf(80), (Integer) output.get("dst_port"));
        assertNotNull(output.get("dst_bytes"));
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
        testFile("DPI_Logs/Metadata_Module/SMTP/smtp_sample_1.json");
    }
}
