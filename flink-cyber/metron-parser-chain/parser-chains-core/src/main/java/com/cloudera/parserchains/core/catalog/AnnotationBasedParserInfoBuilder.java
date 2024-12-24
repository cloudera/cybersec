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

import com.cloudera.parserchains.core.Parser;
import java.util.Optional;
import lombok.extern.slf4j.Slf4j;

/**
 * Constructs a {@link ParserInfo} object using
 * the {@link MessageParser} annotation.
 */
@Slf4j
public class AnnotationBasedParserInfoBuilder implements ParserInfoBuilder {

    public Optional<ParserInfo> build(Class<?> clazz) {
        Optional<ParserInfo> result = Optional.empty();
        MessageParser annotation = clazz.getAnnotation(MessageParser.class);
        if (annotation == null) {
            log.warn("Found parser class missing the '{}' annotation; class={}",
                  MessageParser.class.getName(), clazz.getName());

        } else if (!Parser.class.isAssignableFrom(clazz)) {
            log.warn("Found parser class that does not implement '{}'; class={}",
                  Parser.class.getName(), clazz.getName());

        } else {
            // found a parser.  the cast is safe because of the 'if' condition above
            @SuppressWarnings("unchecked")
            Class<Parser> parserClass = (Class<Parser>) clazz;
            ParserInfo parserInfo = ParserInfo.builder()
                                              .name(annotation.name())
                                              .description(annotation.description())
                                              .parserClass(parserClass)
                                              .build();
            result = Optional.of(parserInfo);
        }
        return result;
    }
}
