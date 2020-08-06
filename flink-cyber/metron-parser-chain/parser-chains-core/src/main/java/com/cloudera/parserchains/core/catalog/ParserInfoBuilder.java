package com.cloudera.parserchains.core.catalog;

import java.util.Optional;

/**
 * Constructs a {@link ParserInfo} object.
 */
public interface ParserInfoBuilder {

    /**
     * Builds a {@link ParserInfo} object from a {@link Class}.
     * @param clazz The class definition.
     * @return A {@link ParserInfo} object, if the given class defines a valid parser. Otherwise, empty.
     */
    Optional<ParserInfo> build(Class<?> clazz);
}
