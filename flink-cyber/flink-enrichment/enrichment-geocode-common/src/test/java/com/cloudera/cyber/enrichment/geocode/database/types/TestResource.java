package com.cloudera.cyber.enrichment.geocode.database.types;

import org.junit.jupiter.api.Assertions;

import java.io.File;
import java.net.URISyntaxException;
import java.net.URL;

public class TestResource {

    public String getFilePath(String resourcePath) throws URISyntaxException {
        URL resource = getClass().getClassLoader().getResource(resourcePath);
        Assertions.assertNotNull(resource);
        return new File(resource.toURI()).getAbsolutePath();
    }
}
