package com.cloudera.parserchains.queryservice.service;


import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcsfService {

    public static final String OCSF_SCHEMA_JSON = "ocsf-schema.json";

    public String getOcsfSchemaString() {
        return new String(readLocalFilePath(OCSF_SCHEMA_JSON));
    }

    private byte[] readLocalFilePath(String fileName) {
        ClassLoader classLoader = getClass().getClassLoader();
        URL filePath = classLoader.getResource(fileName);
        if (filePath == null) {
            throw new RuntimeException("Could not find file: " + fileName);
        }
        try {
            URI uri = filePath.toURI();
            if ("jar".equals(uri.getScheme())) {
                // Handle the case where the resource is inside a JAR
                String[] parts = uri.toString().split("!");
                URI jarUri = URI.create(parts[0]);
                try (FileSystem fs = FileSystems.newFileSystem(jarUri, Collections.emptyMap())) {
                    return Files.readAllBytes(fs.getPath(parts[1]));
                } catch (IOException e) {
                    throw new RuntimeException(e);
                }
            } else {
                // Handle the case where the resource is not in a JAR
                return Files.readAllBytes(Paths.get(uri));
            }
        } catch (URISyntaxException | IOException e) {
            throw new RuntimeException(e);
        }
    }

}
