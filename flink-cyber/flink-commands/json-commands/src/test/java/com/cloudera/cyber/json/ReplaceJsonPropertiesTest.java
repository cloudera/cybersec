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


import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.collect.ImmutableMap;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class ReplaceJsonPropertiesTest {

    @Rule
    public TemporaryFolder folder = new TemporaryFolder();

    @Test
    public void testJsonReplace() throws IOException {
        File outputFile = folder.newFile("enrichment-rest.json.template");
        String[] args = new String[]{getFileResource("/enrichment-rest.json.template"), getFileResource("/rest.properties"), outputFile.getAbsolutePath()};
        ReplaceJsonProperties.main(args);
        List<Map<String, String>> actualPropertyMaps = getProperties(outputFile.getAbsolutePath());
        List<Map<String, String>> expectedPropertyMaps = new ArrayList<>();
        expectedPropertyMaps.add(ImmutableMap.<String, String>builder()
                .put("server", "myhost:1080")
                .put("protocol", "http")
                .put("dga_access_key", "dga_model_key")
                .put("bearer_token", "btoken_value")
                .build());

        expectedPropertyMaps.add(ImmutableMap.<String, String>builder()
                .put("server", "myhost:1080")
                .put("protocol", "http")
                .put("other_access_key", "other_model_key")
                .put("bearer_token", "btoken_value")
                .put("empty_prop", "")
                .build());

        Assert.assertEquals(expectedPropertyMaps, actualPropertyMaps);
    }

    @Test
    public void testTemplateDoesntExist() throws IOException {
        File outputFile = folder.newFile("enrichment-rest.json.template");
        String missingFile = "/missing_file.template";
        String[] args = new String[]{missingFile, getFileResource("/rest.properties"), outputFile.getAbsolutePath()};
        assertThatThrownBy(() -> ReplaceJsonProperties.main(args)).isInstanceOf(NoSuchFileException.class).hasMessageContaining(missingFile);
    }

    @Test
    public void testPropertiesDoesntExist() throws IOException {
        File outputFile = folder.newFile("enrichment-rest.json.template");
        String missingFile = "/missing_file.template";
        String[] args = new String[]{getFileResource("/enrichment-rest.json.template"), missingFile, outputFile.getAbsolutePath()};
        assertThatThrownBy(() -> ReplaceJsonProperties.main(args)).isInstanceOf(FileNotFoundException.class).hasMessageContaining(missingFile);
    }

    @Test
    public void testCantWriteOutputFile() {
        String badOutputFile ="dirdoesntexist/file.json";
        String[] args = new String[]{getFileResource("/enrichment-rest.json.template"), getFileResource("/rest.properties"), badOutputFile};
        assertThatThrownBy(() -> ReplaceJsonProperties.main(args)).isInstanceOf(FileNotFoundException.class).hasMessageContaining(badOutputFile);
    }

    private List<Map<String, String>> getProperties(String file) throws IOException {
        byte[] jsonBytes = Files.readAllBytes(Paths.get(file));
        List<Map<String, String>> propertyMaps = new ArrayList<>();
        ObjectMapper objectMapper = new ObjectMapper();
        JsonNode rootNode = objectMapper.readTree(jsonBytes);
        for (Iterator<JsonNode> rootIter = rootNode.elements(); rootIter.hasNext(); ) {
            JsonNode nextConfig = rootIter.next();
            JsonNode propertiesNode = nextConfig.get("properties");
            if (propertiesNode != null && propertiesNode.isObject()) {
                Map<String, String> props = new HashMap<>();
                ObjectNode propertiesObject = (ObjectNode) propertiesNode;
                for (Iterator<Map.Entry<String, JsonNode>> propIter = propertiesObject.fields(); propIter.hasNext(); ) {
                    Map.Entry<String, JsonNode> prop = propIter.next();
                    props.put(prop.getKey(), prop.getValue().textValue());
                }
                propertyMaps.add(props);
            }
        }
        return propertyMaps;
    }

    private String getFileResource(String path) {
        return getClass().getResource(path).getFile();
    }
}
