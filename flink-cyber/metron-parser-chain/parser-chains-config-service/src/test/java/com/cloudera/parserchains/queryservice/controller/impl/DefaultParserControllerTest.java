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

package com.cloudera.parserchains.queryservice.controller.impl;

import com.cloudera.parserchains.core.catalog.WidgetType;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.parsers.SyslogParser;
import com.cloudera.parserchains.parsers.TimestampParser;
import com.cloudera.parserchains.queryservice.common.utils.CollectionsUtils;
import com.cloudera.parserchains.queryservice.model.describe.ConfigParamDescriptor;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
import com.cloudera.parserchains.queryservice.service.ParserDiscoveryService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_FORM_CONFIG_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_TYPES_URL;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class DefaultParserControllerTest {

  @Autowired
  private MockMvc mvc;

  @MockBean
  private ParserDiscoveryService parserDiscoveryService;

  @Test
  public void list_all_parsers() throws Exception {
    List<ParserSummary> parsers = Arrays.asList(
            new ParserSummary()
                    .setId(ParserID.of(SyslogParser.class))
                    .setName("Syslog"),
            new ParserSummary()
                    .setId(ParserID.of(TimestampParser.class))
                    .setName("Timestamp"));
    given(parserDiscoveryService.findAll()).willReturn(parsers);
    mvc.perform(MockMvcRequestBuilders.get(API_PARSER_TYPES_URL)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.*", instanceOf(List.class)))
            .andExpect(jsonPath("$.*", hasSize(2)))
            .andExpect(jsonPath("$.[0].id", is("com.cloudera.parserchains.parsers.SyslogParser")))
            .andExpect(jsonPath("$.[0].name", is("Syslog")))
            .andExpect(jsonPath("$.[1].id", is("com.cloudera.parserchains.parsers.TimestampParser")))
            .andExpect(jsonPath("$.[1].name", is("Timestamp")));
  }

  @Test
  public void describe_all_parsers() throws Exception {
    ConfigParamDescriptor fieldOne = new ConfigParamDescriptor()
            .setName("outputField")
            .setDescription("The name of the output field.")
            .setLabel("Output Field")
            .setPath("config")
            .setRequired(true)
            .setType(WidgetType.TEXT);
    ConfigParamDescriptor fieldTwo = new ConfigParamDescriptor()
            .setName("inputField")
            .setDescription("The name of the input field.")
            .setLabel("Input Field")
            .setPath("config")
            .setRequired(true)
            .setType(WidgetType.TEXT);
    ParserSummary type1 = new ParserSummary()
            .setId(ParserID.of(SyslogParser.class))
            .setName("Syslog");
    ParserDescriptor schema1 = new ParserDescriptor()
            .setParserID(type1.getId())
            .setParserName(type1.getName())
            .addConfiguration(fieldOne)
            .addConfiguration(fieldTwo);
    ParserSummary type2 = new ParserSummary()
            .setId(ParserID.of(TimestampParser.class))
            .setName("Timestamp");
    ParserDescriptor schema2 = new ParserDescriptor()
            .setParserID(type2.getId())
            .setParserName(type2.getName())
            .addConfiguration(fieldOne)
            .addConfiguration(fieldTwo);
    Map<ParserID, ParserDescriptor> toReturn = CollectionsUtils.toMap(
            type1.getId(), schema1,
            type2.getId(), schema2
    );
    given(parserDiscoveryService.describeAll()).willReturn(toReturn);
    mvc.perform(MockMvcRequestBuilders.get(API_PARSER_FORM_CONFIG_URL)
            .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
            .andExpect(jsonPath("$.*", instanceOf(List.class)))
            .andExpect(jsonPath("$.*", hasSize(2)))

            // Syslog parser
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].name", is("Syslog")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].id", is("com.cloudera.parserchains.parsers.SyslogParser")))

            // Syslog parser, 1st argument
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[0].name", is("outputField")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[0].type", is("text")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[0].label", is("Output Field")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[0].description", is("The name of the output field.")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[0].required", is(true)))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[0].path", is("config")))

            // Syslog parser, 2nd argument
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[1].name", is("inputField")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[1].type", is("text")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[1].label", is("Input Field")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[1].description", is("The name of the input field.")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[1].required", is(true)))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.SyslogParser'].schemaItems.[1].path", is("config")))

            // Timestamp parser
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].name", is("Timestamp")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].id", is("com.cloudera.parserchains.parsers.TimestampParser")))

            // Timestamp parser, 1st argument
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[0].name", is("outputField")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[0].type", is("text")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[0].label", is("Output Field")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[0].description", is("The name of the output field.")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[0].required", is(true)))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[0].path", is("config")))

            // Timestamp parser, 2nd argument
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[1].name", is("inputField")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[1].type", is("text")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[1].label", is("Input Field")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[1].description", is("The name of the input field.")))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[1].required", is(true)))
            .andExpect(jsonPath("$.['com.cloudera.parserchains.parsers.TimestampParser'].schemaItems.[1].path", is("config")));
  }
}
