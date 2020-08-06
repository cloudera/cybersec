package com.cloudera.parserchains.core.catalog;

import java.util.List;

/**
 * Provides a catalog of all parsers available to the user.
 */
public interface ParserCatalog {

    /**
     * Returns all of the available parsers in the catalog.
     */
    List<ParserInfo> getParsers();
}
