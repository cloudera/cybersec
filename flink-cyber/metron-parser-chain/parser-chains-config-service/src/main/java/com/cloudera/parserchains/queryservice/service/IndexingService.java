package com.cloudera.parserchains.queryservice.service;

import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import com.cloudera.parserchains.core.utils.JSONUtils;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class IndexingService {

    public Object getMappingsFromPath(String path) throws IOException {
        final JSONUtils.ReferenceSupplier<Map<String, MappingDto>> ref =
              new JSONUtils.ReferenceSupplier<Map<String, MappingDto>>() {
              };
        return getDataMap(path, ref, Map.class);
    }

    public Object getTableConfigFromPath(String path) throws IOException {
        final JSONUtils.ReferenceSupplier<Map<String, List<TableColumnDto>>> ref =
              new JSONUtils.ReferenceSupplier<Map<String, List<TableColumnDto>>>() {
              };
        return getDataMap(path, ref, Map.class);
    }

    private static <R> R getDataMap(String path, JSONUtils.ReferenceSupplier<?> validationRef, Class<R> resultClass)
          throws IOException {
        final Path indexingPath = new Path(path);
        FileSystem fileSystem = indexingPath.getFileSystem();
        if (!fileSystem.exists(indexingPath)) {
            return null;
        }
        try (FSDataInputStream fsDataInputStream = fileSystem.open(indexingPath)) {
            final String chainString = IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);

            //validate the json value is the valid json
            JSONUtils.INSTANCE.load(chainString, validationRef);
            //Converting to map so that we ignore all the overwritten getters
            return JSONUtils.INSTANCE.getMapper().readValue(chainString, resultClass);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Wasn't able to read the index file [%s]!", path), e);
        }
    }

    public void saveDataToPath(String path, Object data) throws IOException {
        final Path indexingPath = new Path(path);
        FileSystem fileSystem = indexingPath.getFileSystem();
        if (!fileSystem.exists(indexingPath.getParent())) {
            return;
        }
        try (FSDataOutputStream fsDataOutputStream = fileSystem.create(indexingPath, FileSystem.WriteMode.OVERWRITE)) {
            JSONUtils.INSTANCE.getMapper().writeValue(fsDataOutputStream, data);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Wasn't able to save the file [%s]!", path), e);
        }
    }
}
