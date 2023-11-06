/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.cloudera.parserchains.queryservice.service;

import com.cloudera.parserchains.core.catalog.AnnotationBasedParserInfoBuilder;
import com.cloudera.parserchains.core.catalog.ParserCatalog;
import com.cloudera.parserchains.core.catalog.ParserInfo;
import com.cloudera.parserchains.core.catalog.ParserInfoBuilder;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.parsers.DelimitedTextParser;
import com.cloudera.parserchains.parsers.RemoveFieldParser;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummaryMapper;
import com.cloudera.parserchains.queryservice.service.impl.DefaultParserDiscoveryService;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.text.IsEqualCompressingWhiteSpace.equalToCompressingWhiteSpace;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class DefaultParserDiscoveryServiceTest {
  private ParserDiscoveryService service;
  private ParserSummaryMapper mapper;
  @Mock private ParserCatalog catalog;

  @BeforeEach
  public void beforeEach() {
    setupParserCatalog();
    mapper = new ParserSummaryMapper();
    service = new DefaultParserDiscoveryService(catalog, mapper);
  }

  private void setupParserCatalog() {
    // add a few of the demo parsers to a 'test' catalog to use during the tests
    List<ParserInfo> testCatalog = new ArrayList<>();
    ParserInfoBuilder infoBuilder = new AnnotationBasedParserInfoBuilder();
    infoBuilder.build(DelimitedTextParser.class)
            .ifPresent(info -> testCatalog.add(info));
    infoBuilder.build(RemoveFieldParser.class)
            .ifPresent(info -> testCatalog.add(info));
    when(catalog.getParsers())
            .thenReturn(testCatalog);
  }

  @Test
  void findAll() throws IOException {
    List<ParserSummary> actual = service.findAll();
    List<ParserSummary> expected = catalog.getParsers()
            .stream()
            .map(info -> mapper.reform(info))
            .collect(Collectors.toList());
    assertThat(actual, equalTo(expected));
    assertThat(actual.size(), equalTo(2));
  }

  /**
   * {
   *   "id" : "com.cloudera.parserchains.parsers.DelimitedTextParser",
   *   "name" : "Delimited Text",
   *   "schemaItems" : [ {
   *     "name" : "delimiter",
   *     "type" : "text",
   *     "label" : "Delimiter",
   *     "description" : "A regex used to split the text. Default value: ','",
   *     "required" : false,
   *     "multipleValues" : false,
   *     "path" : "config.delimiter",
   *     "multiple" : true,
   *     "defaultValue" : [ {
   *       "delimiter" : ","
   *     } ],
   *     "outputName" : false
   *   }, {
   *     "name" : "fieldIndex",
   *     "type" : "text",
   *     "label" : "Column Index",
   *     "description" : "The index of the column containing the data.",
   *     "required" : true,
   *     "multipleValues" : true,
   *     "path" : "config.outputField",
   *     "multiple" : true,
   *     "outputName" : false
   *   }, {
   *     "name" : "fieldName",
   *     "type" : "text",
   *     "label" : "Field Name",
   *     "description" : "The name of the output field.",
   *     "required" : true,
   *     "multipleValues" : true,
   *     "path" : "config.outputField",
   *     "multiple" : true,
   *     "outputName" : true
   *   }, {
   *     "name" : "inputField",
   *     "type" : "text",
   *     "label" : "Input Field",
   *     "description" : "The name of the input field to parse. Default value: 'original_string'",
   *     "required" : false,
   *     "multipleValues" : false,
   *     "path" : "config.inputField",
   *     "multiple" : true,
   *     "defaultValue" : [ {
   *       "inputField" : "original_string"
   *     } ],
   *     "outputName" : false
   *   }, {
   *     "name" : "trim",
   *     "type" : "text",
   *     "label" : "Trim Whitespace",
   *     "description" : "Trim whitespace from each value. Default value: 'true'",
   *     "required" : false,
   *     "multipleValues" : false,
   *     "path" : "config.trim",
   *     "multiple" : true,
   *     "defaultValue" : [ {
   *       "trim" : "true"
   *     } ],
   *     "outputName" : false
   *   } ]
   * }
   */
  @Multiline
  String describeExpected;

  @Test
  void describe() throws IOException {
    ParserID parserId = ParserID.of(DelimitedTextParser.class);
    ParserDescriptor schema = service.describe(parserId);
    String actual = JSONUtils.INSTANCE.toJSON(schema, true);
    assertThat(actual, equalToCompressingWhiteSpace(describeExpected));
  }

  /**
   * {
   *   "com.cloudera.parserchains.parsers.RemoveFieldParser" : {
   *     "id" : "com.cloudera.parserchains.parsers.RemoveFieldParser",
   *     "name" : "Remove Field(s)",
   *     "schemaItems" : [ {
   *       "name" : "fieldToRemove",
   *       "type" : "text",
   *       "label" : "Field to Remove",
   *       "description" : "The name of a field to remove.",
   *       "required" : true,
   *       "multipleValues" : true,
   *       "path" : "config.fieldToRemove",
   *       "multiple" : true,
   *       "outputName" : false
   *     } ]
   *   },
   *   "com.cloudera.parserchains.parsers.DelimitedTextParser" : {
   *     "id" : "com.cloudera.parserchains.parsers.DelimitedTextParser",
   *     "name" : "Delimited Text",
   *     "schemaItems" : [ {
   *       "name" : "delimiter",
   *       "type" : "text",
   *       "label" : "Delimiter",
   *       "description" : "A regex used to split the text. Default value: ','",
   *       "required" : false,
   *       "multipleValues" : false,
   *       "path" : "config.delimiter",
   *       "multiple" : true,
   *       "defaultValue" : [ {
   *         "delimiter" : ","
   *       } ],
   *       "outputName" : false
   *     }, {
   *       "name" : "fieldIndex",
   *       "type" : "text",
   *       "label" : "Column Index",
   *       "description" : "The index of the column containing the data.",
   *       "required" : true,
   *       "multipleValues" : true,
   *       "path" : "config.outputField",
   *       "multiple" : true,
   *       "outputName" : false
   *     }, {
   *       "name" : "fieldName",
   *       "type" : "text",
   *       "label" : "Field Name",
   *       "description" : "The name of the output field.",
   *       "required" : true,
   *       "multipleValues" : true,
   *       "path" : "config.outputField",
   *       "multiple" : true,
   *       "outputName" : true
   *     }, {
   *       "name" : "inputField",
   *       "type" : "text",
   *       "label" : "Input Field",
   *       "description" : "The name of the input field to parse. Default value: 'original_string'",
   *       "required" : false,
   *       "multipleValues" : false,
   *       "path" : "config.inputField",
   *       "multiple" : true,
   *       "defaultValue" : [ {
   *         "inputField" : "original_string"
   *       } ],
   *       "outputName" : false
   *     }, {
   *       "name" : "trim",
   *       "type" : "text",
   *       "label" : "Trim Whitespace",
   *       "description" : "Trim whitespace from each value. Default value: 'true'",
   *       "required" : false,
   *       "multipleValues" : false,
   *       "path" : "config.trim",
   *       "multiple" : true,
   *       "defaultValue" : [ {
   *         "trim" : "true"
   *       } ],
   *       "outputName" : false
   *     } ]
   *   }
   * }
   */
  @Multiline
  String describeAllExpected;

  @Test
  void describeAll() throws IOException {
    Map<ParserID, ParserDescriptor> schema = service.describeAll();
    String actual = JSONUtils.INSTANCE.toJSON(schema, true);
    assertThat(actual, equalToCompressingWhiteSpace(describeAllExpected));
  }
}
