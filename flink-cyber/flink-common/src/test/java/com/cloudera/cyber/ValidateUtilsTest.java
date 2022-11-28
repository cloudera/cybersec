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

package com.cloudera.cyber;

import com.cloudera.cyber.flink.Utils;
import org.apache.flink.api.java.utils.ParameterTool;
import org.junit.Test;

import static org.assertj.core.api.Assertions.*;


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

    @Test
    public void testPropToolsMerge() {
        ParameterTool parameterTool = Utils.getParamToolsFromProperties(
                new String[]{"src/test/resources/test.properties", "src/test/resources/test1.properties", "src/test/resources/test2.properties"});

        assertThat(parameterTool.toMap()).containsOnly(
                entry("test.property", "test-property-rewrited"),
                entry("test.property1", "test-property1"),
                entry("test1.property", "test1-property"),
                entry("test1.property1", "test1-property1-rewrited"),
                entry("test2.property", "test2-property"),
                entry("test2.property1", "test2-property1")
                );
    }
}