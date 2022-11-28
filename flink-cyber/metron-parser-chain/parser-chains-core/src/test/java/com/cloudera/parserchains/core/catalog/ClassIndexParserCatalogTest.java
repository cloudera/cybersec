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

package com.cloudera.parserchains.core.catalog;

import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

public class ClassIndexParserCatalogTest {

    @MessageParser(name="Fake Parser", description="Parser created for catalog tests.")
    private static class FakeParser implements Parser {
        @Override
        public Message parse(Message message) {
            // do nothing
            return null;
        }
    }

    @Test
    void findParser() {
        ParserCatalog catalog = new ClassIndexParserCatalog();
        List<ParserInfo> parsers = catalog.getParsers();
        boolean foundFakeParser = false;
        for(ParserInfo parserInfo: parsers) {
            if(FakeParser.class.equals(parserInfo.getParserClass())) {
                // found the fake parser
                foundFakeParser = true;
                assertEquals("Fake Parser", parserInfo.getName());
                assertEquals("Parser created for catalog tests.", parserInfo.getDescription());
            }
        }
        assertTrue(foundFakeParser);
    }

    /**
     * A parser that does not implement {@link Parser}.  This is not a valid parser.
     */
    @MessageParser(name="Bad Parser", description="A description.")
    private static class BadParser { }

    @Test
    void badParser() {
        ParserCatalog catalog = new ClassIndexParserCatalog();
        List<ParserInfo> parsers = catalog.getParsers();
        for(ParserInfo parserInfo: parsers) {
            if(BadParser.class.equals(parserInfo.getParserClass())) {
                fail("Should not have 'found' this bad parser.");
            }
        }
    }
}
