package com.cloudera.cyber.profiler.sql.catalog;

import org.apache.flink.table.catalog.Catalog;
import org.apache.flink.table.factories.CatalogFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ProfileSourceCatalogFactory implements CatalogFactory {
    @Override
    public Catalog createCatalog(String name,
                                 Map<String,String> properties) {
        return new ProfileSourceCatalog(properties);
    }

    @Override
    public Map<String, String> requiredContext() {
        return null;
    }

    @Override
    public List<String> supportedProperties() {
        return Arrays.asList("kafka.bootstrap-servers", "schema-registry.address");
    }
}
