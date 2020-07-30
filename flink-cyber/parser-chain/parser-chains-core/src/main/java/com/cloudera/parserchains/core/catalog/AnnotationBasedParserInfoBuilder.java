package com.cloudera.parserchains.core.catalog;

import com.cloudera.parserchains.core.Parser;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.Optional;

/**
 * Constructs a {@link ParserInfo} object using
 * the {@link MessageParser} annotation.
 */
public class AnnotationBasedParserInfoBuilder implements ParserInfoBuilder {
    private static final Logger logger = LogManager.getLogger(AnnotationBasedParserInfoBuilder.class);

    public Optional<ParserInfo> build(Class<?> clazz) {
        Optional<ParserInfo> result = Optional.empty();
        MessageParser annotation = clazz.getAnnotation(MessageParser.class);
        if(annotation == null) {
            logger.warn("Found parser class missing the '{}' annotation; class={}",
                    MessageParser.class.getName(), clazz.getName());

        } else if(!Parser.class.isAssignableFrom(clazz)) {
            logger.warn("Found parser class that does not implement '{}'; class={}",
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
