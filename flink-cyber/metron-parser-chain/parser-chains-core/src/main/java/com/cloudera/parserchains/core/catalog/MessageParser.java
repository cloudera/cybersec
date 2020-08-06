package com.cloudera.parserchains.core.catalog;

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import org.atteo.classindex.IndexAnnotated;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

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
     * @return
     */
    String description() default "";
}
