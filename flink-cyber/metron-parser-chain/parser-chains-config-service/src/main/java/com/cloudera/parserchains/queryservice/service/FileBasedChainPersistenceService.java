/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.common.utils.IDGenerator;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

@Service
@Slf4j
public class FileBasedChainPersistenceService implements ChainPersistenceService {

  @Autowired
  private IDGenerator<Long> idGenerator;

  /**
   * Only return json files.
   */
  private DirectoryStream.Filter<Path> fileFilter = new DirectoryStream.Filter<Path>() {
    @Override
    public boolean accept(Path entry) throws IOException {
      PathMatcher matcher = FileSystems.getDefault().getPathMatcher("glob:*.json");
      // can't compare only the entry bc the globbing won't cross directory boundaries. We only care about the filename here, anyhow.
      return matcher.matches(entry.getFileName());
    }
  };

  @Autowired
  public FileBasedChainPersistenceService(IDGenerator<Long> idGenerator) {
    this.idGenerator = idGenerator;
  }

  @Override
  public List<ParserChainSummary> findAll(Path path) throws IOException {
    if (!Files.exists(path)) {
      Files.createDirectories(path);
    }
    List<Path> inputs = getFilesSorted(path);
    List<ParserChainSummary> summaries = new ArrayList<>();
    for (Path file : inputs) {
      try {
        ParserChainSchema chain = JSONUtils.INSTANCE.load(file.toFile(), ParserChainSchema.class);
        summaries.add(new ParserChainSummary(chain));
      } catch (IOException ioe) {
        log.warn(
            "Found a file in the config directory that was unable to be deserialized as a parser chain: '{}'",
            file, ioe);
      }
    }
    return summaries;
  }

  private List<Path> getFilesSorted(Path path) throws IOException {
    List<Path> inputs = new ArrayList<>();
    try (DirectoryStream<Path> files = Files.newDirectoryStream(path, fileFilter)) {
      for (Path file : files) {
        inputs.add(file);
      }
    }
    sortByName(inputs);
    return inputs;
  }

  private void sortByName(List<Path> paths) {
    paths.sort(Comparator.comparing(p -> p.getFileName().toString()));
  }

  @Override
  public ParserChainSchema create(ParserChainSchema chain, Path path) throws IOException {
    validateChain(chain, path);

    String newId = Long.toString(idGenerator.incrementAndGet());
    chain.setId(newId);
    writeChain(chain, path);
    return chain;
  }

  private void validateChain(ParserChainSchema chain, Path path) throws IOException {
    validateChain(null, chain, path);
  }

  private void validateChain(String id, ParserChainSchema chain, Path path) throws IOException {
    if (chain == null){
      throw new RuntimeException("Provided chain can't be null!");
    }

    final boolean duplicateName = findAll(path).stream()
            //If we're changing the chain without changing its name, it won't be counted as a duplicate
            .anyMatch(chainSummary -> !chainSummary.getId().equals(id) && chainSummary.getName().equals(chain.getName()));
    if (duplicateName){
      throw new RuntimeException("Duplicate chain names are restricted!");
    }
  }

  private void writeChain(ParserChainSchema chain, Path outPath) throws IOException {
    Path out = Paths.get(getFileName(chain.getId()));
    out = outPath.resolve(out);
    byte[] bytes = JSONUtils.INSTANCE.toJSONPretty(chain);
    Files.write(out, bytes);
  }

  private String getFileName(String id) {
    return id + ".json";
  }

  @Override
  public ParserChainSchema read(String id, Path path) throws IOException {
    Path inPath = findFile(id, path);
    if (null == inPath) {
      return null;
    }
    return JSONUtils.INSTANCE.load(inPath.toFile(), ParserChainSchema.class);
  }

  private Path findFile(String id, Path root) throws IOException {
    try (DirectoryStream<Path> files = Files.newDirectoryStream(root)) {
      for (Path file : files) {
        if (file.getFileName().toString().equals(getFileName(id))) {
          return file;
        }
      }
    }
    return null;
  }

  @Override
  public ParserChainSchema update(String id, ParserChainSchema chain, Path path) throws IOException {
    validateChain(id, chain, path);

    ParserChainSchema readChain = read(id, path);
    if (null == readChain) {
      return null;
    }
    // enforce that the client cannot overwrite the chain ID
    chain.setId(id);
    writeChain(chain, path);
    return read(id, path);
  }

  @Override
  public boolean delete(String id, Path path) throws IOException {
    Path deletePath = findFile(id, path);
    if (null == deletePath) {
      return false;
    }
    Files.delete(deletePath);
    return true;
  }
}
