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

package com.cloudera.parserchains.core.utils;

import lombok.experimental.UtilityClass;
import org.apache.commons.lang3.StringEscapeUtils;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@UtilityClass
public final class StringUtils {

    private static JSONUtils jsonUtils = JSONUtils.INSTANCE;

    public static char getFirstChar(String delimiter) {
        return unescapeJava(delimiter).charAt(0);
    }

    public static String unescapeJava(String text) {
        return StringEscapeUtils.unescapeJava(text);
    }

    public static Object parseProperType(String s) {
        Optional<?> result;
        result = getLong(s);
        if (result.isPresent()){
            return result.get();
        }
        result = getDouble(s);
        if (result.isPresent()){
            return result.get();
        }
        result = getList(s);
        if (result.isPresent()){
            return result.get();
        }
        result = getMap(s);
        if (result.isPresent()){
            return result.get();
        }
        return s;
    }

    public static Optional<Double> getDouble(String text){
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Double.parseDouble(text));
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }

    public static Optional<Long> getLong(String text){
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(text.trim()));
        } catch (NumberFormatException nfe) {
            return Optional.empty();
        }
    }

    public static Optional<List<Object>> getList(String text){
        if (text == null || getMap(text).isPresent()) {
            return Optional.empty();
        }
        try {
            return Optional.of(jsonUtils.load(text, List.class));
        } catch (IOException nfe) {
            return Optional.empty();
        }
    }

    public static Optional<Map<Object,Object>> getMap(String text){
        if (text == null) {
            return Optional.empty();
        }
        try {
            return Optional.of(jsonUtils.load(text, Map.class));
        } catch (IOException nfe) {
            return Optional.empty();
        }
    }

}
