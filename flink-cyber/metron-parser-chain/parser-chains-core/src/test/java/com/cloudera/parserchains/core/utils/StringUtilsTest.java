package com.cloudera.parserchains.core.utils;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Optional;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

class StringUtilsTest {

    @Test
    public void testGetDouble() {
        assertThat("Expected proper double value.",
                StringUtils.getDouble("abc"), is(Optional.empty()));
        assertThat("Expected proper double value.",
                StringUtils.getDouble("1.1d"), is(Optional.of(1.1d)));
        assertThat("Expected proper double value.",
                StringUtils.getDouble("1,1"), is(Optional.empty()));
        assertThat("Expected proper double value.",
                StringUtils.getDouble("1 1"), is(Optional.empty()));
        assertThat("Expected proper double value.",
                StringUtils.getDouble("abc 1.1"), is(Optional.empty()));
        assertThat("Expected proper double value.",
                StringUtils.getDouble(" 1.1"), is(Optional.of(1.1d)));
        assertThat("Expected proper double value.",
                StringUtils.getDouble("1.1 "), is(Optional.of(1.1d)));
        assertThat("Expected proper double value.",
                StringUtils.getDouble("1"), is(Optional.of(1d)));
        assertThat("Expected proper double value.",
                StringUtils.getDouble("1.1"), is(Optional.of(1.1d)));
    }

    @Test
    public void testGetLong() {
        assertThat("Expected proper long value.",
                StringUtils.getLong("abc"), is(Optional.empty()));
        assertThat("Expected proper long value.",
                StringUtils.getLong("1l"), is(Optional.empty()));
        assertThat("Expected proper long value.",
                StringUtils.getLong("1L"), is(Optional.empty()));
        assertThat("Expected proper long value.",
                StringUtils.getLong("1,1"), is(Optional.empty()));
        assertThat("Expected proper long value.",
                StringUtils.getLong("1 1"), is(Optional.empty()));
        assertThat("Expected proper long value.",
                StringUtils.getLong("abc 1"), is(Optional.empty()));
        assertThat("Expected proper long value.",
                StringUtils.getLong(" 1"), is(Optional.of(1L)));
        assertThat("Expected proper long value.",
                StringUtils.getLong("1 "), is(Optional.of(1L)));
        assertThat("Expected proper long value.",
                StringUtils.getLong("1"), is(Optional.of(1L)));
        assertThat("Expected proper long value.",
                StringUtils.getLong("1.1"), is(Optional.empty()));
    }

    @Test
    public void testGetList() {
        assertThat("Expected proper list.",
                StringUtils.getList("abc"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("1l"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("[]"), is(Optional.of(new ArrayList())));
        assertThat("Expected proper list.",
                StringUtils.getList("["), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("]"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("[abc]"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("['abc']"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("[\"abc\"]"), is(Optional.of(Arrays.asList("abc"))));
        assertThat("Expected proper list.",
                StringUtils.getList("[\"abc\",\"def\"]"), is(Optional.of(Arrays.asList("abc", "def"))));
        assertThat("Expected proper list.",
                StringUtils.getList("{}"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("{'test'}"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("{'test':'value'}"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("{\"test\"}"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getList("{\"test\":\"value\"}"), is(Optional.empty()));
    }

    @Test
    public void testGetMap() {
        assertThat("Expected proper list.",
                StringUtils.getMap("abc"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("1l"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("[]"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("["), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("]"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("[abc]"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("['abc']"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("[\"abc\"]"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("[\"abc\",\"def\"]"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("{}"), is(Optional.of(new HashMap())));
        assertThat("Expected proper list.",
                StringUtils.getMap("{'test'}"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("{'test':'value'}"), is(Optional.empty()));
        assertThat("Expected proper list.",
                StringUtils.getMap("{\"test\"}"), is(Optional.empty()));
        final HashMap map = new HashMap();
        map.put("test","value");
        assertThat("Expected proper list.",
                StringUtils.getMap("{\"test\":\"value\"}"), is(Optional.of(map)));
    }

}