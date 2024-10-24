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

package com.cloudera.parserchains.core.catalog;

import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.atteo.classindex.ClassIndex;

/**
 * A {@link ParserCatalog} that builds a catalog of parsers using a class index
 * compiled at build time.
 *
 * <p>A parser must be marked using the {@link MessageParser} annotation
 * so that the parser is discoverable using this class.
 *
 * <p>
 * https://github.com/atteo/classindex
 */
@Slf4j
public class ClassIndexParserCatalog implements ParserCatalog {
    private final ParserInfoBuilder parserInfoBuilder;

    public ClassIndexParserCatalog(ParserInfoBuilder parserInfoBuilder) {
        this.parserInfoBuilder = parserInfoBuilder;
    }

    public ClassIndexParserCatalog() {
        this(new AnnotationBasedParserInfoBuilder());
    }

    @Override
    public List<ParserInfo> getParsers() {
        List<ParserInfo> results = new ArrayList<>();

        // search the class index for the annotation
        Iterable<Class<?>> knownAnnotations = ClassIndex.getAnnotated(MessageParser.class);
        for (Class<?> clazz : knownAnnotations) {
            parserInfoBuilder.build(clazz).ifPresent(info -> results.add(info));
        }

        if (log.isDebugEnabled()) {
            for (ParserInfo parserInfo : results) {
                log.debug("Found parser: class={}, name={}, desc={}",
                      parserInfo.getParserClass(),
                      parserInfo.getName(),
                      parserInfo.getDescription());
            }
        }
        return results;
    }
}
