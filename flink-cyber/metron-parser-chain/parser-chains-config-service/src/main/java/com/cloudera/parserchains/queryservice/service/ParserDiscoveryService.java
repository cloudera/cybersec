package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A service that finds and describes the parsers that are available.
 */
public interface ParserDiscoveryService {

  /**
   * Finds all of the parser types available for the user to build
   * parser chains with.
   */
  List<ParserSummary> findAll() throws IOException;

  /**
   * Describes the configuration parameters available for a given parser.
   * @param name The parser name.
   */
  ParserDescriptor describe(ParserID name) throws IOException;

  /**
   * Describes the configuration parameters for all available parser
   * types.
   */
  Map<ParserID, ParserDescriptor> describeAll() throws IOException;
}
