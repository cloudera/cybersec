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

package com.cloudera.parserchains.queryservice.model.describe;


import com.cloudera.parserchains.core.catalog.WidgetType;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.parsers.SyslogParser;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;

public class ParserDescriptorTest {

    /**
     * {
     *   "id" : "com.cloudera.parserchains.parsers.SyslogParser",
     *   "name" : "Syslog",
     *   "schemaItems" : [ {
     *     "name" : "outputField",
     *     "type" : "text",
     *     "label" : "Output Field",
     *     "description" : "The name of the output field.",
     *     "required" : true,
     *     "multipleValues" : false,
     *     "path" : "config",
     *     "multiple" : false,
     *     "outputName" : false
     *   }, {
     *     "name" : "inputField",
     *     "type" : "text",
     *     "label" : "Input Field",
     *     "description" : "The name of the input field.",
     *     "required" : true,
     *     "multipleValues" : false,
     *     "path" : "config",
     *     "multiple" : false,
     *     "defaultValue" : [ {
     *       "outputField" : "original_string"
     *     } ],
     *     "outputName" : false
     *   } ]
     * }
      */
    @Multiline
    private String expectedJSON;

    @Test
    void toJSON() throws Exception {
        ParserDescriptor schema = new ParserDescriptor()
                .setParserID(ParserID.of(SyslogParser.class))
                .setParserName(ParserName.of("Syslog"))
                .addConfiguration(new ConfigParamDescriptor()
                        .setName("outputField")
                        .setDescription("The name of the output field.")
                        .setLabel("Output Field")
                        .setPath("config")
                        .setRequired(true)
                        .setType(WidgetType.TEXT))
                .addConfiguration(new ConfigParamDescriptor()
                        .setName("inputField")
                        .setDescription("The name of the input field.")
                        .setLabel("Input Field")
                        .setPath("config")
                        .setRequired(true)
                        .setType(WidgetType.TEXT)
                        .addDefaultValue("outputField", "original_string"));
        String actual = JSONUtils.INSTANCE.toJSON(schema, true);
        assertThat(actual, equalToCompressingWhiteSpace(expectedJSON));
    }

}
