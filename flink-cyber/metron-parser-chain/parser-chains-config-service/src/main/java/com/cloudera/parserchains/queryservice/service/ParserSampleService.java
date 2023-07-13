package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.model.sample.ParserSample;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FSDataOutputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.springframework.stereotype.Service;

import java.io.BufferedOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ParserSampleService {

    public List<ParserSample> findAllById(String sampleFolderPath, String id) throws IOException {
        final String samplePathStr = sampleFolderPath.concat(id).concat(".json");
        final Path samplePath = new Path(samplePathStr);
        FileSystem fileSystem = samplePath.getFileSystem();
        if (!fileSystem.exists(samplePath)) {
            return null;
        }
        try (FSDataInputStream fsDataInputStream = fileSystem.open(samplePath)) {
            final String chainString = IOUtils.toString(fsDataInputStream, StandardCharsets.UTF_8);
            final JSONUtils.ReferenceSupplier<List<ParserSample>> ref = new JSONUtils.ReferenceSupplier<List<ParserSample>>() {
            };
            return JSONUtils.INSTANCE.load(chainString, ref);
        } catch (Exception e) {
            throw new RuntimeException(String.format("Wasn't able to read the chain sample file [%s]!", samplePathStr), e);
        }
    }

    public List<ParserSample> update(String sampleFolderPath, String id, List<ParserSample> sampleList) throws IOException {
        final String samplePathStr = sampleFolderPath.concat(id).concat(".json");
        final Path samplePath = new Path(samplePathStr);
        FileSystem fileSystem = samplePath.getFileSystem();
        try (FSDataOutputStream fsDataOutputStream = fileSystem.create(samplePath, FileSystem.WriteMode.OVERWRITE);
             DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(fsDataOutputStream))) {
            final String json = JSONUtils.INSTANCE.toJSON(sampleList, true);

            dataOutputStream.write(json.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(String.format("Wasn't able to create the chain sample file [%s]!", samplePathStr), e);
        }
        return findAllById(sampleFolderPath, id);
    }
}
