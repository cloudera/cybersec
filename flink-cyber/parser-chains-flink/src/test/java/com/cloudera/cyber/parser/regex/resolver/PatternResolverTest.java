package com.cloudera.cyber.parser.regex.resolver;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.AssertionsForClassTypes.assertThat;
import static org.assertj.core.api.AssertionsForClassTypes.assertThatThrownBy;

public class PatternResolverTest {

    @Test
    public void testMatch() {
        final Map<String, String> patternToString = new HashMap<>();
        final String number = "number";
        final String alpha = "alpha";
        final String alphaNumeric = "alphanumeric";
        patternToString.put("[0-9]*", number);
        patternToString.put("[a-z]*", alpha);

        PatternResolver<String> patternResolver = new PatternResolver<>(patternToString);
        // test patterns in the resolver
        assertThat(patternResolver.match("12345", v -> null)).isEqualTo(number);
        assertThat(patternResolver.match("abcde", v -> null)).isEqualTo(alpha);
        // test unresolved pattern that returns the default
        assertThat(patternResolver.match("abc123", v -> alphaNumeric)).isEqualTo(alphaNumeric);
    }

    @Test
    public void testInvalidRegex() {
        final Map<String, String> badRegexToString = new HashMap<>();
        badRegexToString.put("[", "value");

        assertThatThrownBy(() -> new PatternResolver<>(badRegexToString)).isInstanceOf(IllegalArgumentException.class);
    }
}
