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

import com.cloudera.parserchains.core.Message;
import com.jayway.jsonpath.InvalidPathException;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static com.cloudera.parserchains.core.Constants.DEFAULT_INPUT_FIELD;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.assertThrows;

public class JSONPathParserTest {
    JSONPathParser parser;

    @BeforeEach
    void beforeEach() {
        parser = new JSONPathParser();
    }

    /**
     * {
     *     "store": {
     *         "book": [
     *             {
     *                 "category": "reference",
     *                 "author": "Nigel Rees",
     *                 "title": "Sayings of the Century",
     *                 "price": 8.95
     *             },
     *             {
     *                 "category": "fiction",
     *                 "author": "Evelyn Waugh",
     *                 "title": "Sword of Honour",
     *                 "price": 12.99
     *             },
     *             {
     *                 "category": "fiction",
     *                 "author": "Herman Melville",
     *                 "title": "Moby Dick",
     *                 "isbn": "0-553-21311-3",
     *                 "price": 8.99
     *             },
     *             {
     *                 "category": "fiction",
     *                 "author": "J. R. R. Tolkien",
     *                 "title": "The Lord of the Rings",
     *                 "isbn": "0-395-19395-8",
     *                 "price": 22.99
     *             }
     *         ],
     *         "bicycle": {
     *             "color": "red",
     *             "price": 19.95
     *         }
     *     },
     *     "expensive": 10
     * }
     */
    @Multiline
    static String json;

    @Test
    void expression() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("numberOfBooks", "$..book.length()")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("numberOfBooks", "4")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleSingleString() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("author", "$.store.book[1].author")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("author", "Evelyn Waugh")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleListOfMaps() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("books", "$.store.book[*]")
                .parse(input);

        Message expected = Message.builder()
                .withFields(input)
                .addField("books", "[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99},{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\",\"author\":\"J. R. R. Tolkien\",\"title\":\"The Lord of the Rings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}]")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleListOfMapsDeepScan() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("books", "$..store.book[*]")
                .parse(input);

        Message expected = Message.builder()
                .withFields(input)
                .addField("books", "[{\"category\":\"reference\",\"author\":\"Nigel Rees\",\"title\":\"Sayings of the Century\",\"price\":8.95},{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99},{\"category\":\"fiction\",\"author\":\"Herman Melville\",\"title\":\"Moby Dick\",\"isbn\":\"0-553-21311-3\",\"price\":8.99},{\"category\":\"fiction\",\"author\":\"J. R. R. Tolkien\",\"title\":\"The Lord of the Rings\",\"isbn\":\"0-395-19395-8\",\"price\":22.99}]")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleList() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("authors", "$.store.book[*].author")
                .parse(input);

        Message expected = Message.builder()
                .withFields(input)
                .addField("authors", "[\"Nigel Rees\",\"Evelyn Waugh\",\"Herman Melville\",\"J. R. R. Tolkien\"]")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleListDeepScan() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("authors", "$..store.book[*].author")
                .parse(input);

        Message expected = Message.builder()
                .withFields(input)
                .addField("authors", "[\"Nigel Rees\",\"Evelyn Waugh\",\"Herman Melville\",\"J. R. R. Tolkien\"]")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleSet() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("bookKeys", "$.store.book[0].keys()")
                .parse(input);

        Message expected = Message.builder()
                .withFields(input)
                .addField("bookKeys", "[\"category\",\"author\",\"title\",\"price\"]")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleSetDeepScan() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("rootKeys", "$..store.book[0].keys()")
                .parse(input);

        Message expected = Message.builder()
                .withFields(input)
                .addField("rootKeys", "[\"store\",\"expensive\"]")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleDouble() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("maxPrice", "$..store.book[*].price.max()")
                .parse(input);

        Message expected = Message.builder()
                .withFields(input)
                .addField("maxPrice", "22.99")
                .build();
        assertThat(output, is(expected));
    }

    //CYB-91
    @Test
    void handleMap() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("book_info", "$.store.book[1]")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("book_info", "{\"category\":\"fiction\",\"author\":\"Evelyn Waugh\",\"title\":\"Sword of Honour\",\"price\":12.99}")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void expressions() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("book1", "$..book[0].title")
                .expression("book2", "$..book[1].title")
                .expression("book3", "$..book[2].title")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("book1", "Sayings of the Century")
                .addField("book2", "Sword of Honour")
                .addField("book3", "Moby Dick")
                .build();
        assertThat(output, is(expected));
    }

    @Test
    void noMatch() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("date", "$..book[0].date")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("date", "")
                .build();
        assertThat("Expected the field to have a blank value, if there is no match.",
                output, is(expected));
    }

    @Test
    void invalidJsonPath() {
        assertThrows(InvalidPathException.class,
                () -> parser.expression("author", "$.store.book[?(@.price > 10)"));
    }

    @Test
    void predicate() {
        Message input = Message.builder()
                .addField(DEFAULT_INPUT_FIELD, json)
                .build();
        Message output = parser
                .expression("expensiveTitle", "$.store.book[?(@.price > 20)].title")
                .parse(input);
        Message expected = Message.builder()
                .withFields(input)
                .addField("expensiveTitle", "The Lord of the Rings")
                .build();
        assertThat(output, is(expected));
    }
}
