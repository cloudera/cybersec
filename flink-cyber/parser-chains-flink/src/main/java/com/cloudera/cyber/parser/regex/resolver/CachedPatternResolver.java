package com.cloudera.cyber.parser.regex.resolver;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

/**
 * Pattern resolver that maps regex patterns to values.
 *
 * Use a caching to skip matching when the same key will be matched over and over.
 *
 * @param <T> The value type.
 */
public class CachedPatternResolver<T> extends PatternResolver<T> {
    Map<String, T> cachedMap;

    public CachedPatternResolver(Map<String, T> patternToTypeMap) {
        super(patternToTypeMap);
        this.cachedMap = new HashMap<>();
    }

    public T match(String stringToMatch, Function<? super String, ? extends T> createDefault) {
        return cachedMap.computeIfAbsent(stringToMatch, s -> super.match(s, createDefault));
    }
}
