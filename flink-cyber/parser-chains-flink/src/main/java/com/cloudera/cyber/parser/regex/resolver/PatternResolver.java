package com.cloudera.cyber.parser.regex.resolver;

import org.apache.flink.api.java.tuple.Tuple2;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.regex.PatternSyntaxException;
import java.util.stream.Collectors;

/**
 * Maps regular expressions to values.
 *
 * @param <T>
 */
public class PatternResolver<T> {
    List<Tuple2<Pattern, T>> patterns;

    /**
     * Constructor
     * Throws PatternSyntaxException if pattern doesn't compile.
     *
     * @param patternToTypeMap Map of pattern strings to values.
     */
    public PatternResolver(Map<String, T> patternToTypeMap) {
        // compile the regex strings to java patterns
        patterns = patternToTypeMap.entrySet().stream().map(e -> new Tuple2<>(compilePattern(e.getKey()), e.getValue())).
                collect(Collectors.toList());
    }

    /**
     * Compile a pattern adding context to any errors thrown.  The configs contain multiple patterns.
     * Add the text of the pattern that failed compilation so the user will be able to fix it.
     * @param patternString The string of the
     * @return
     */
    private static Pattern compilePattern(String patternString) {
        // there are multiple patterns in the configs
        // add context about which pattern failed compilation
        try {
            return Pattern.compile(patternString);
        } catch (PatternSyntaxException syntaxException) {
            throw new IllegalArgumentException(String.format("Pattern '%s' did not compile.", patternString), syntaxException);
        }
    }

    /**
     * Find the pattern that matches stringToMatch.  If no match is found call the createDefault function.
     *
     * @param stringToMatch Attempt to match this string to a stored pattern.
     * @param createDefault If no match is found, call this function to create a default value.
     * @return The match or default value.
     */
    public T match(String stringToMatch, Function<? super String, ? extends T> createDefault) {
        return patterns.stream().filter(s -> s.f0.matcher(stringToMatch).matches()).findFirst().map(t -> t.f1).orElse(createDefault.apply(stringToMatch));
    }
}
