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

package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.Constants;
import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.Message;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.ZoneOffset;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

public class GrokParserTest {
    private GrokParser grokParser;

    @BeforeEach
    void beforeEach() {
        grokParser = new GrokParser();
    }

    @Test
    void expression() {
        final String textToParse = "Foo Bar Baz 7F8C7CB0-4786-11E3-8F96-0800200C9A66";
        Message input = Message.builder()
                .addField("input", textToParse)
                .build();
        Message output = grokParser
                .inputField(FieldName.of("input"))
                .expression("%{UUID}")
                .parse(input);
        Message expected = Message.builder()
                .addField("input", textToParse)
                .addField("UUID", "7F8C7CB0-4786-11E3-8F96-0800200C9A66")
                .build();
        assertThat("Expected to find a UUID.",
                output, is(expected));
    }

    @Test
    void complexExpression() {
        final String textToParse = "2004/03/07 test 64.242.88.10:8080";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = grokParser
                .expression("%{DATA:date;date;yyyy/MM/dd} %{USERNAME:username:text} %{IPORHOST:host}:%{POSINT:port:integer}")
                .parse(input);
        Message expected = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .addField("date", "2004-03-07T00:00:00Z")
                .addField("username", "test")
                .addField("host", "64.242.88.10")
                .addField("port", "8080")
                .build();
        assertThat("Expected to match on HTTPDATE, USERNAME, IPORHOST, and POSINT.",
                output, is(expected));
    }

    @Test
    void multipleExpressions() {
        final String textToParse = "www.google.com:80 Foo Bar Baz 7F8C7CB0-4786-11E3-8F96-0800200C9A66";
        Message input = Message.builder()
                .addField("input", textToParse)
                .build();
        Message output = grokParser
                .inputField(FieldName.of("input"))
                .expression("%{UUID}")
                .expression("%{HOSTPORT}")
                .parse(input);
        Message expected = Message.builder()
                .addField("input", textToParse)
                .addField("UUID", "7F8C7CB0-4786-11E3-8F96-0800200C9A66")
                .addField("HOSTPORT", "www.google.com:80")
                .addField("IPORHOST", "www.google.com")
                .addField("PORT", "80")
                .build();
        assertThat("Expected to find a UUID and HOSTPORT.",
                output, is(expected));
    }

    @Test
    void setInputField() {
        final String textToParse = "Foo Bar Baz 7F8C7CB0-4786-11E3-8F96-0800200C9A66";
        Message input = Message.builder()
                .addField("input", textToParse)
                .build();
        Message output = grokParser
                .inputField(FieldName.of("input"))
                .expression("%{UUID}")
                .expression("%{HOSTPORT}")
                .parse(input);
        Message expected = Message.builder()
                .addField("input", textToParse)
                .addField("UUID", "7F8C7CB0-4786-11E3-8F96-0800200C9A66")
                .build();
        assertThat("Expected to parse the input field 'input'.",
                output, is(expected));
    }

    @Test
    void missingInputField() {
        Message input = Message.builder()
                .addField("foo", "bar")
                .build();
        Message output = grokParser
                .inputField(FieldName.of("input"))
                .expression("%{UUID}")
                .parse(input);
        assertThat("Expected all the input fields to remain.",
                output.getFields(), is(input.getFields()));
        assertThat("Expected an error to have occurred.",
                output.getError().isPresent(), is(true));
    }

    @Test
    void defaultInputField() {
        final String textToParse = "7F8C7CB0-4786-11E3-8F96-0800200C9A66";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = grokParser
                .expression("%{UUID}")
                .parse(input);
        Message expected = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .addField("UUID", "7F8C7CB0-4786-11E3-8F96-0800200C9A66")
                .build();
        assertThat("Expected the default input field to have been used.",
                output, is(expected));
    }

    @Test
    void defaultPatterns() {
        final String apacheLog = "112.169.19.192 - - [06/Mar/2013:01:36:30 +0900] " +
                "\"GET / HTTP/1.1\" 200 44346 \"-\" " +
                "\"Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) " +
                "AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.152 Safari/537.22\"";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, apacheLog)
                .build();
        Message output = grokParser
                .expression("%{COMBINEDAPACHELOG}")
                .parse(input);
        Message expected = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, apacheLog)
                .addField("response", "200")
                .addField("clientip", "112.169.19.192")
                .addField("timestamp", "06/Mar/2013:01:36:30 +0900")
                .addField("verb", "GET")
                .addField("agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_8_2) AppleWebKit/537.22 (KHTML, like Gecko) Chrome/25.0.1364.152 Safari/537.22")
                .addField("request", "/")
                .addField("auth", "-")
                .addField("ident", "-")
                .addField("referrer", "-")
                .addField("bytes", "44346")
                .addField("httpversion", "1.1")
                .addField("COMMONAPACHELOG", "112.169.19.192 - - [06/Mar/2013:01:36:30 +0900] \"GET / HTTP/1.1\" 200 44346")
                .addField("COMBINEDAPACHELOG", apacheLog)
                .addField("TIME", "01:36:30")
                .addField("INT", "+0900")
                .addField("MINUTE", "36")
                .addField("SECOND", "30")
                .addField("MONTHDAY", "06")
                .addField("MONTH", "Mar")
                .addField("HOUR", "01")
                .addField("YEAR", "2013")
                .build();
        assertThat("Expected the Apache log to have been parsed using a default pattern.",
                output, is(expected));
    }

    @Test
    void configureInputField() {
        final String fieldName = "some_input_field";
        grokParser.inputField(fieldName);
        assertThat("Expected the input field to have been configured.",
                grokParser.getInputField(), is(FieldName.of(fieldName)));
    }

    @Test
    void configureExpression() {
        grokParser.expression( "%{UUID}");
        grokParser.expression("%{HOSTPORT}");
        assertThat("Expected 2 Grok expressions.",
                grokParser.getGrokExpressions().size(), is(2));
    }

    @Test
    void configureZoneOffset() {
        final String offset = "+02:00";
        ZoneOffset expected = ZoneOffset.of(offset);
        grokParser.zoneOffset(offset);
        assertThat("Expected the zone offset to have been configured.",
                grokParser.getZoneOffset(), is(expected));
    }

    @Test
    void grokPattern() {
        final String textToParse = "Orange Cucumber";
        Message input = Message.builder()
                .addField(Constants.DEFAULT_INPUT_FIELD, textToParse)
                .build();
        Message output = grokParser
                .pattern("FRUIT", "Orange|Lemon")
                .pattern("VEGETABLE", "Cucumber|Broccoli")
                .expression("%{FRUIT:fruit} %{VEGETABLE:veggie}")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("fruit", "Orange")
                .addField("veggie", "Cucumber")
                .build();
        assertThat(output, is(expected));
    }
}
