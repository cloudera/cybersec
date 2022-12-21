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

package com.cloudera.cyber.enrichment.threatq;

import com.cyber.jackson.core.JsonProcessingException;
import com.cyber.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.stream.Stream;

@Slf4j
public class ThreatQParser {
    private static DateFormat df = new SimpleDateFormat("dd-MM-yyyy hh:mm:ss");
    private static ObjectMapper om;

    static {
        om = new ObjectMapper();
        om.setDateFormat(df);
    }

    /**
     * The threatQ sample is almost JSON, but broken in a variety of ways
     *
     * @param input
     * @return
     */
    public static Stream<ThreatQEntry> parse(InputStream input) throws IOException {
        Stream<String> output = new BufferedReader(new InputStreamReader(input, StandardCharsets.UTF_8)).lines();

        return output.map(str -> str
                .replace("'", "\"")
                .replace(": None,", ":null,"))
                .map(str -> {
                    try {
                        return om.readValue(str, ThreatQEntry.class);
                    } catch (JsonProcessingException e) {
                        log.error("Parsing failed", e);
                        return null;
                    }
                })
                .filter(r -> r != null);
    }
}
