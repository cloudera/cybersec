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

package com.cloudera.cyber.enrichemnt.stellar;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichemnt.stellar.functions.flink.StellarEnrichMapFunction;
import com.cloudera.cyber.enrichment.geocode.IpGeoJob;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.ArrayUtils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.environment.StreamExecutionEnvironment;

@Slf4j
public abstract class StellarEnrichmentJob {
    protected static final String STELLAR_ENRICHMENT_GROUP_ID = "enrichment-stellar";

    public static final String PARAMS_CONFIG_DIR = "stellar.config.dir";


    public static DataStream<Message> enrich(DataStream<Message> source, Map<String, String> sensorConfigs,
                                             String geoDatabasePath, String asnDatabasePath) {
        return source.map(new StellarEnrichMapFunction(sensorConfigs, geoDatabasePath, asnDatabasePath))
                     .name("Stellar Enrichment Mapper").uid("stellar-flat-map");
    }

    protected StreamExecutionEnvironment createPipeline(ParameterTool params) throws IOException {
        StreamExecutionEnvironment env = StreamExecutionEnvironment.getExecutionEnvironment();
        String configFileDir = params.getRequired(PARAMS_CONFIG_DIR);
        DataStream<Message> source = createSource(env, params);

        DataStream<Message> result =
              enrich(source, loadFiles(configFileDir), params.getRequired(IpGeoJob.PARAM_GEO_DATABASE_PATH),
                    params.getRequired(IpGeoJob.PARAM_ASN_DATABASE_PATH));
        writeResults(env, params, result);
        return env;
    }

    public static Map<String, String> loadFiles(String pathToFiles) throws IOException {
        Map<String, String> result = new HashMap<>();
        FileSystem fileSystem = new Path(pathToFiles).getFileSystem();
        final FileStatus[] fileStatusList = fileSystem.listStatus(new Path(pathToFiles));
        if (ArrayUtils.isEmpty(fileStatusList)) {
            throw new RuntimeException(
                  String.format("Provided config directory doesn't exist or empty [%s]!", pathToFiles));
        }
        Path[] configPaths = Arrays.stream(fileStatusList).map(FileStatus::getPath)
                                   .filter(path -> FilenameUtils.isExtension(path.getName(), "json"))
                                   .toArray(Path[]::new);
        for (Path path : configPaths) {
            result.put(FilenameUtils.removeExtension(path.getName()), readConfigFile(path));
        }
        return result;
    }

    private static String readConfigFile(Path path) throws IOException {
        try (FSDataInputStream fsDataInputStream = path.getFileSystem().open(path)) {
            log.info("Successfully loaded file {}", path);
            return IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);
        } catch (IOException ioe) {
            log.error("Exception while loading file " + path, ioe);
            throw ioe;
        }
    }

    protected abstract void writeResults(StreamExecutionEnvironment env, ParameterTool params,
                                         DataStream<Message> result);

    protected abstract DataStream<Message> createSource(StreamExecutionEnvironment env, ParameterTool params);


}
