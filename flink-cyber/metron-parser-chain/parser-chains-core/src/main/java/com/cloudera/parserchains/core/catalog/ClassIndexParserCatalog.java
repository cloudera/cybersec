package com.cloudera.parserchains.core.catalog;

import lombok.extern.slf4j.Slf4j;
import org.atteo.classindex.ClassIndex;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link ParserCatalog} that builds a catalog of parsers using a class index
 * compiled at build time.
 *
 * <p>A parser must be marked using the {@link MessageParser} annotation
 * so that the parser is discoverable using this class.
 *
 * https://github.com/atteo/classindex
 */
@Slf4j
public class ClassIndexParserCatalog implements ParserCatalog {
    private ParserInfoBuilder parserInfoBuilder;

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
        for(Class<?> clazz: knownAnnotations) {
            parserInfoBuilder.build(clazz).ifPresent(info -> results.add(info));
        }

        if(log.isDebugEnabled()) {
            for(ParserInfo parserInfo: results) {
                log.debug("Found parser: class={}, name={}, desc={}",
                        parserInfo.getParserClass(),
                        parserInfo.getName(),
                        parserInfo.getDescription());
            }
        }
        return results;
    }
}
