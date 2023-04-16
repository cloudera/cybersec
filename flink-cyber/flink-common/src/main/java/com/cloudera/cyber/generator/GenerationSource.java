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

import com.cloudera.cyber.generator.scenario.GeneratorScenario;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.io.IOUtils;
import org.apache.flink.core.fs.Path;

import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.Charset;
import java.util.Collections;
import java.util.Map;
import java.util.Objects;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GenerationSource implements Serializable {
    private String file;
    private String topic;
    private String outputAvroSchemaFile;
    private Double weight;
    private String scenarioFile = null;
    private String outputAvroSchema = null;
    private GeneratorScenario scenario = null;


    public GenerationSource(String file, String topic, String outputAvroSchemaFile, Double weight) {
        this.file = file;
        this.topic = topic;
        this.outputAvroSchemaFile = outputAvroSchemaFile;
        this.weight = weight;
    }

    public void readAvroSchema() throws IOException {
        if (outputAvroSchemaFile != null) {
            final Path schemaPath = new Path(outputAvroSchemaFile);
            try (InputStream schemaStream = schemaPath.getFileSystem().open(schemaPath)) {
                outputAvroSchema = IOUtils.toString(
                        Objects.requireNonNull(schemaStream), Charset.defaultCharset());
            }
        }
    }

    public void readScenarioFile() throws IOException {
        if (scenarioFile != null) {
            scenario = GeneratorScenario.load(scenarioFile);
        }
    }

    public Map<String, String> getRandomParameters() throws IOException {
        if (scenarioFile != null) {
            return scenario.randomParameters();
        } else {
            return Collections.emptyMap();
        }
    }
}
