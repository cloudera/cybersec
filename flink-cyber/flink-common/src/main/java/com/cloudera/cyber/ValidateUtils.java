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

import org.springframework.util.CollectionUtils;

import java.util.Collection;
import java.util.Collections;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public final class ValidateUtils {

    private static final Pattern PHOENIX_NAME_REGEXP = Pattern.compile("^[a-zA-Z_][a-zA-Z0-9_]+");
    private static final Pattern UNDERSCORE_PATTERN = Pattern.compile("_+");

    private ValidateUtils() {
    }

    public static void validatePhoenixName(String value, String parameter) {
        if (!PHOENIX_NAME_REGEXP.matcher(value).matches() || UNDERSCORE_PATTERN.matcher(value).matches()) {
            throw new IllegalArgumentException(String.format("Invalid value %s for parameter '%s'. It can only contain alphanumerics or underscore(a-z, A-Z, 0-9, _)", value, parameter));
        }
    }

    public static <T, R> Collection<R> getDuplicates(Collection<T> list, Function<T, R> classifier) {
        if (CollectionUtils.isEmpty(list)) {
            return Collections.emptyList();
        }
        return list.stream()
            .collect(Collectors.groupingBy(classifier, Collectors.counting()))
            .entrySet()
            .stream().filter(entry -> entry.getValue() > 1)
            .map(Entry::getKey)
            .collect(Collectors.toList());
    }

    public static <T> Collection<T> getDuplicates(Collection<T> collections) {
        return getDuplicates(collections, Function.identity());
    }


    public static class ValidationException extends RuntimeException {

        public ValidationException() {
            super();
        }

        public ValidationException(String message) {
            super(message);
        }

        public ValidationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
