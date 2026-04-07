package com.cloudera.cyber.parser.regex.resolver;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.*;

import java.util.HashMap;
import java.util.Map;

public class CachedPatternResolverTest {
    private static final String KEY_PREFIX = "key";

    /**
     * Test caching and retrieving non-null values.
     */
    @Test
    public void testNonNullResolverValue() {
        final Map<String, Integer> strToInt = new HashMap<>();
        final int value1 = 1;
        final String key1 = String.format("%s%d", KEY_PREFIX, value1);

        // map "key1" but leave "key2" unmapped
        // "key2" will get the default value
        strToInt.put(key1, value1);

        CachedPatternResolver<Integer> cachedPatternResolver = new CachedPatternResolver<>(strToInt);

        retrieveFromOriginalAndThenFromCache(cachedPatternResolver, key1, value1);
        retrieveFromOriginalAndThenFromCache(cachedPatternResolver, "key2", 2);
    }

    /**
     * Retrieve the value twice.  The first call retrieves the original or puts a default value in the cache.
     * The second retrieves the cached value.
     *
     * @param cachedPatternResolver The resolver under test.
     * @param key                   The key to retrieve.
     * @param expectedValue         The expected value.
     */
    private static void retrieveFromOriginalAndThenFromCache(CachedPatternResolver<Integer> cachedPatternResolver, String key, Integer expectedValue) {
        assertThat(cachedPatternResolver.match(key, CachedPatternResolverTest::convertKeyToDefaultValue)).isEqualTo(expectedValue);
        assertThat(cachedPatternResolver.match(key, CachedPatternResolverTest::convertKeyToDefaultValue)).isEqualTo(expectedValue);
    }

    /**
     * Create a default value for the key.
     *
     * @param key The key to retrieve.
     * @return The default value for the key.
     */
    private static int convertKeyToDefaultValue(String key) {
        return Integer.parseInt(key.substring(KEY_PREFIX.length()));
    }

    /**
     * Test caching and retrieving null values.
     */
    @Test
    public void testNullResolverValue() {
        CachedPatternResolver<Integer> cachedPatternResolver = new CachedPatternResolver<>(new HashMap<>());
        final String testKey = "key";
        assertThat(cachedPatternResolver.match(testKey, k -> null)).isNull();
        assertThat(cachedPatternResolver.match(testKey, k -> null)).isNull();
    }


}
