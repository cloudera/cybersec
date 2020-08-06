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
