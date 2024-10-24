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

package com.cloudera.parserchains.queryservice.model.summary;

import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.model.define.ParserID;

public class ParserSummaryMapper implements ObjectMapper<ParserSummary, ParserInfo> {

    @Override
    public ParserSummary reform(ParserInfo source) {
        return new ParserSummary()
              .setId(ParserID.of(source.getParserClass()))
              .setName(source.getName())
              .setDescription(source.getDescription());
    }

    @Override
    public ParserInfo transform(ParserSummary source) {
        String clazzName = source.getId().getId();
        Class<?> clazz;
        try {
            clazz = Class.forName(clazzName);
        } catch (ClassNotFoundException e) {
            String msg = String.format("Parser class not found; class=%s", clazzName);
            throw new IllegalArgumentException(msg, e);
        }

        if (Parser.class.isAssignableFrom(clazz)) {
            // the cast is guaranteed to be safe because of the 'if' condition above
            @SuppressWarnings("unchecked")
            Class<com.cloudera.parserchains.core.Parser> parserClass = (Class<Parser>) clazz;
            return ParserInfo.builder()
                             .parserClass(parserClass)
                             .name(source.getName().getName())
                             .description(source.getDescription())
                             .build();

        } else {
            String msg = String.format("Parser class is not a valid parser; class=%s", clazzName);
            throw new IllegalArgumentException(msg);
        }
    }
}
