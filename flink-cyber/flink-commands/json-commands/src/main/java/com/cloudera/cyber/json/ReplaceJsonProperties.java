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

package com.cloudera.cyber.json;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.extern.slf4j.Slf4j;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.Properties;

@Slf4j
public class ReplaceJsonProperties {

    public static void main(String[] args) throws IOException {
        if (args.length != 3) {
            log.error("Requires <original_json_file> <property_file> <new_json_file> parameters.");
            System.exit(1);
        }

        String templateRestConfigPath = args[0];
        String propertyFile = args[1];
        String generatedRestConfigPath = args[2];

        Properties restProperties = new Properties();
        try (InputStream restPropertyStream = new FileInputStream(propertyFile)) {
            restProperties.load(restPropertyStream);
            injectEnvironmentProperties(templateRestConfigPath, generatedRestConfigPath, restProperties);
        }
    }

    private static void injectEnvironmentProperties(String templateRestConfigPath, String generatedRestConfigPath, Properties restProperties) throws IOException {

        byte[] templateBytes = Files.readAllBytes(Paths.get(templateRestConfigPath));

        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(templateBytes);
        for (Iterator<JsonNode> rootIter = rootNode.elements(); rootIter.hasNext(); ) {
            JsonNode nextConfig = rootIter.next();
            JsonNode propertiesNode = nextConfig.get("properties");
            if (propertiesNode != null && propertiesNode.isObject()) {
                ObjectNode propertiesObject = (ObjectNode)propertiesNode;
                for (String propertyName : restProperties.stringPropertyNames()) {
                    if (propertiesObject.has(propertyName)) {
                        propertiesObject.put(propertyName, restProperties.getProperty(propertyName));
                    }
                }
            }
        }

        objectMapper.writerWithDefaultPrettyPrinter().writeValue(new File(generatedRestConfigPath), rootNode);
    }
}
