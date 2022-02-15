package com.cloudera.cyber;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;


public class ValidateUtilsTest {

    private final String testParam = "testParam";

    @Test
    public void validPhoenixNameTest() {
        assertThatCode(() -> ValidateUtils.validatePhoenixName("name", testParam)).doesNotThrowAnyException();
        assertThatCode(() -> ValidateUtils.validatePhoenixName("name1", testParam)).doesNotThrowAnyException();
        assertThatCode(() -> ValidateUtils.validatePhoenixName("_name_", testParam)).doesNotThrowAnyException();
        assertThatCode(() -> ValidateUtils.validatePhoenixName("_name1", testParam)).doesNotThrowAnyException();
        assertThatCode(() -> ValidateUtils.validatePhoenixName("name1", testParam)).doesNotThrowAnyException();
        assertThatCode(() -> ValidateUtils.validatePhoenixName("n1_a_me1", testParam)).doesNotThrowAnyException();
    }

    @Test
    public void invalidPhoenixNameTest() {
        assertThatThrownBy(() -> ValidateUtils.validatePhoenixName("1name", testParam)).isInstanceOf(IllegalArgumentException.class).hasMessage("Invalid value 1name for parameter '" + testParam + "'. It can only contain alphanumerics or underscore(a-z, A-Z, 0-9, _)");
        assertThatThrownBy(() -> ValidateUtils.validatePhoenixName("$name1", testParam)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidateUtils.validatePhoenixName("_n^ame_", testParam)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidateUtils.validatePhoenixName("!name1", testParam)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidateUtils.validatePhoenixName("n###ame1", testParam)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ValidateUtils.validatePhoenixName("__________", testParam)).isInstanceOf(IllegalArgumentException.class);
    }
}