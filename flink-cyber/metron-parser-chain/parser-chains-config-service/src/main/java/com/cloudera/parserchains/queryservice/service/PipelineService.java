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

package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.queryservice.config.AppProperties;
import com.cloudera.parserchains.queryservice.model.exec.PipelineResult;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.core.fs.FileStatus;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class PipelineService {

  private final AppProperties appProperties;
  private final CacheManager cacheManager;

  @Cacheable("pipelinePathMap")
  public Map<String, PipelineResult> findAll() throws IOException {
    final Path pipelineRootPath = getPipelineRootPath();

    final FileSystem fileSystem = pipelineRootPath.getFileSystem();
    if (!fileSystem.exists(pipelineRootPath)) {
      return null;
    }
    final FileStatus[] statusList = fileSystem.listStatus(pipelineRootPath);
    if (statusList == null) {
      return null;
    }

    final Map<String, PipelineResult> pipelineMap = new HashMap<>();
    for (FileStatus fileStatus : statusList) {
      if (fileStatus.isDir()) {
        final Path pipelinePath = fileStatus.getPath();
        if (isValidPipeline(pipelinePath, fileSystem)) {
          final String name = pipelinePath.getName();
          final PipelineResult pipeline = PipelineResult.builder()
              .name(name)
              .path(pipelinePath)
              .build();
          pipelineMap.put(name, pipeline);
        }
      }
    }
    return pipelineMap;
  }

  public PipelineResult createPipeline(String pipelineName) throws IOException {
    final Path pipelineRootPath = getPipelineRootPath();
    final FileSystem fileSystem = pipelineRootPath.getFileSystem();
    if (!fileSystem.exists(pipelineRootPath)) {
      return null;
    }

    final Path pipelinePath = new Path(pipelineRootPath.getPath(), pipelineName);
    final Path fullValidPath = new Path(pipelinePath, "parse/chains");

    if (fileSystem.mkdirs(fullValidPath)) {
      evictPipelineCache();
      return PipelineResult.builder()
          .name(pipelineName)
          .path(fullValidPath)
          .build();
    }
    return null;
  }

  public PipelineResult renamePipeline(String pipelineName, String newName) throws IOException {
    final Path pipelineRootPath = getPipelineRootPath();
    final FileSystem fileSystem = pipelineRootPath.getFileSystem();
    if (!fileSystem.exists(pipelineRootPath)) {
      return null;
    }

    final Path originalPath = new Path(pipelineRootPath, pipelineName);
    if (!isValidPipeline(originalPath, fileSystem)) {
      throw new RuntimeException("Provided original pipeline name doesn't correspond to a valid pipeline!");
    }
    final Path newPath = new Path(pipelineRootPath, newName);

    if (fileSystem.exists(newPath)) {
      throw new RuntimeException("New pipeline name is already occupied!");
    }

    if (fileSystem.rename(originalPath, newPath)) {
      evictPipelineCache();
      return PipelineResult.builder()
          .name(newName)
          .path(newPath)
          .build();
    }

    return null;
  }

  public boolean deletePipeline(String pipelineName) throws IOException {
    final Path pipelineRootPath = getPipelineRootPath();
    final FileSystem fileSystem = pipelineRootPath.getFileSystem();
    if (!fileSystem.exists(pipelineRootPath)) {
      return false;
    }

    final Path pipelinePath = new Path(pipelineRootPath, pipelineName);
    if (!isValidPipeline(pipelinePath, fileSystem)) {
      throw new RuntimeException("Provided pipeline name doesn't correspond to a valid pipeline!");
    }
    if (fileSystem.delete(pipelinePath, true)){
      evictPipelineCache();
      return true;
    }
    return false;
  }

  private boolean isValidPipeline(Path originalPath, FileSystem fileSystem) throws IOException {
    final Path fullPath = new Path(originalPath, "parse/chains");
    return fileSystem.exists(fullPath);
  }

  private Path getPipelineRootPath() {
    String pipelinePathStr = appProperties.getPipelinesPath();
    return new Path(pipelinePathStr);
  }

  private void evictPipelineCache() {
    cacheManager.getCache("pipelinePathMap").clear();
  }

}
