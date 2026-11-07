package com.cloudera.cyber.ondemand.config;

import com.cloudera.cyber.indexing.hive.tableapi.TableApiJobFactory;
import org.apache.flink.api.java.utils.ParameterTool;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class OnDemandEnrichmentConfigurationHelper {

    public static final String INDEX_DATABASE = "cyber";
    public static final String DEFAULT_START_TIME = "2026-09-01 17:15:00.035";
    public static final String DEFAULT_END_TIME = "2026-09-02 18:30:00.000";

    public OndemandEnrichmentConfiguration readConfiguration(Path tempDir, String sources, String destinationDatabase)
            throws Exception {
        return readConfiguration(tempDir, sources, destinationDatabase, TableApiJobFactory.ICEBERG_INDEX_JOB_CONNECTOR,
                DEFAULT_START_TIME, DEFAULT_END_TIME, "hdfs:/path/to/errors");
    }

    public OndemandEnrichmentConfiguration readConfiguration(Path tempDir, String sources, String destinationDatabase, String connector)
            throws Exception {
        return readConfiguration(tempDir, sources, destinationDatabase, connector,
                DEFAULT_START_TIME, DEFAULT_END_TIME, "hdfs:/path/to/errors");
    }

    public OndemandEnrichmentConfiguration readConfiguration(Path tempDir, String sources, String destinationDatabase,
                                                             String startTime, String endTime) throws Exception {
        return readConfiguration(tempDir, sources, destinationDatabase, TableApiJobFactory.ICEBERG_INDEX_JOB_CONNECTOR,
                startTime, endTime, "hdfs:/path/to/errors");
    }

    public OndemandEnrichmentConfiguration readConfiguration(Path tempDir, String sources, String destinationDatabase, String connector,
                                                             String startTimestamp, String endTimestamp, String errorReportPath)
            throws Exception {
        String suffix = connector + "-" + destinationDatabase;

        Path triageFile = tempDir.resolve("triage-" + suffix + ".properties");
        Files.writeString(triageFile, String.join("\n",
                "geo.enabled=false",
                "asn.enabled=false",
                "company.enabled=false",
                "lookups.config.file=" + resourcePath("ondemand-lookups.json")));

        Path indexFile = tempDir.resolve("index-" + suffix + ".properties");
        Files.writeString(indexFile, String.join("\n",
                "flink.output-connector=" + connector,
                "hive.dbname=" + INDEX_DATABASE,
                "flink.mapping-file=" + resourcePath("ondemand-index-mapping.json"),
                "flink.tables-init-file=" + resourcePath("ondemand-index-tables.json")));

        Map<String, String> params = new HashMap<>();
        params.put("sources", sources);
        params.put("triage.config", triageFile.toString());
        params.put("index.config", indexFile.toString());
        params.put("start.timestamp", startTimestamp);
        params.put("end.timestamp", endTimestamp);
        params.put("destination.hive.dbname", destinationDatabase);
        if (errorReportPath != null) {
            params.put("report.errors.path", errorReportPath);
        }

        return OndemandEnrichmentConfiguration.readConfiguration(ParameterTool.fromMap(params));
    }

    private String resourcePath(String name) throws Exception {
        return new File(getClass().getClassLoader().getResource(name).toURI()).getAbsolutePath();
    }

}
