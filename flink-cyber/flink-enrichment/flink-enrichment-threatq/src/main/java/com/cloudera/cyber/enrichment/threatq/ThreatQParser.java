package com.cloudera.cyber.enrichment.threatq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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

    {
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
