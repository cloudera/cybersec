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

package com.cloudera.parserchains.core;

public class Validator {

    private Validator() {
        // do not instantiate this class
    }

    /**
     * Throws an exception if a value does not match a regular expression.
     *
     * @param value      The value that must match.
     * @param regex      The regular expression that defines a valid value.
     * @param entityName The name of the entity being validated.
     * @throws IllegalArgumentException If the value is invalid.
     */
    public static void mustMatch(String value, Regex regex, String entityName) throws IllegalArgumentException {
        if (!regex.matches(value)) {
            throw new IllegalArgumentException(String.format("Invalid %s=%s", entityName, value));
        }
    }
}
