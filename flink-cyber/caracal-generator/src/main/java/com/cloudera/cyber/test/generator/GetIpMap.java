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

package com.cloudera.cyber.test.generator;

import org.apache.flink.api.common.functions.MapFunction;
import org.apache.flink.api.java.tuple.Tuple2;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class GetIpMap implements MapFunction<Tuple2<String, String>, String> {

    private static final Pattern ipExtractPattern = Pattern.compile("([0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3}\\.[0-9]{1,3})");

    @Override
    public String map(Tuple2<String, String> s) {
        Matcher matcher = ipExtractPattern.matcher(s.f1);
        if (matcher.find()) {
            return matcher.group(1);
        } else {
            return null;
        }
    }
}
