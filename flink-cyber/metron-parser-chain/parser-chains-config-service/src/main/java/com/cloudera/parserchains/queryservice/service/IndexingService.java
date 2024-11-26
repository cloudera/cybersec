package com.cloudera.parserchains.queryservice.service;

import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.parserchains.core.utils.JSONUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    public Object getMappingsFromPath(String path) throws IOException {
        final Path indexingPath = new Path(path);
        FileSystem fileSystem = indexingPath.getFileSystem();
        if (!fileSystem.exists(indexingPath)) {
            return null;
        }
        try (FSDataInputStream fsDataInputStream = fileSystem.open(indexingPath)) {
            final String chainString = IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);
            final JSONUtils.ReferenceSupplier<Map<String, MappingDto>> ref = new JSONUtils.ReferenceSupplier<Map<String, MappingDto>>() {
            };
            //validate the json value is the valid mapping json
            JSONUtils.INSTANCE.load(chainString, ref);
            //Converting to map so that we ignore all the overwritten getters
            return JSONUtils.INSTANCE.getMapper().readValue(chainString, Map.class);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Wasn't able to read the index mapping file [%s]!", path), e);
        }
    }

    public void saveMappingsToPath(String path, Map<String, MappingDto> mappings) throws IOException {
        final Path indexingPath = new Path(path);
        FileSystem fileSystem = indexingPath.getFileSystem();
        if (!fileSystem.exists(indexingPath)) {
            return;
        }
        try (FSDataOutputStream fsDataOutputStream = fileSystem.create(indexingPath, FileSystem.WriteMode.OVERWRITE)) {
            JSONUtils.INSTANCE.getMapper().writeValue(fsDataOutputStream, mappings);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Wasn't able to save the index mapping file [%s]!", path), e);
        }
    }
}
