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

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import org.atteo.classindex.IndexAnnotated;

/**
 * A marker for {@link Parser} implementations that parse {@link Message}s.
 *
 * <p>Using this annotation allows a parser builder to provide metadata
 * about their parser.
 *
 * <p>This also makes the parser discoverable by a {@link ParserCatalog}.
 */
@Retention(RetentionPolicy.RUNTIME)
@IndexAnnotated
public @interface MessageParser {

    /**
     * Returns the name of the parser.
     */
    String name() default "";

    /**
     * Returns a description of the parser.
     */
    String description() default "";
}
