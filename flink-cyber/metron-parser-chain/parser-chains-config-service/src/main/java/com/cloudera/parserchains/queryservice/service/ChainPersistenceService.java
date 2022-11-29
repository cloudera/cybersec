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

import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Provides parser chain services.
 */
public interface ChainPersistenceService {

  List<ParserChainSummary> findAll(Path path) throws IOException;

  /**
   * Creates and persists a new parser chain.
   * @param chain The parser chain to save.
   * @param path The path to save the parser chain to.
   * @return
   * @throws IOException
   */
  ParserChainSchema create(ParserChainSchema chain, Path path) throws IOException;

  /**
   * Retrieves a persisted parser chain.
   * @param id The id of the parser chain.
   * @param path The path to save the parser chain to.
   * @return The parser chain with the given id.
   * @throws IOException
   */
  ParserChainSchema read(String id, Path path) throws IOException;

  /**
   * Updates an existing parser chain.
   * @param id The id of the parser chain.
   * @param chain The new definition of the parser chain.
   * @param path The path where the parser chain is persisted.
   * @return
   * @throws IOException
   */
  ParserChainSchema update(String id, ParserChainSchema chain, Path path) throws IOException;

  /**
   * Deletes a persisted parser chain.
   * @param id The id of the parser chain.
   * @param path The path where the parser chain is persisted.
   * @return
   * @throws IOException
   */
  boolean delete(String id, Path path) throws IOException;
}
