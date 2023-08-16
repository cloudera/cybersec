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

package com.cloudera.parserchains.queryservice.controller.impl;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_PROXY_CREATE_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_PROXY_DELETE_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_PROXY_READ_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_PROXY_UPDATE_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_PROXY_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_CHAINS_READ_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_INDEXING_PROXY_URL;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.API_PARSER_PROXY_TEST_URL;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.model.describe.IndexMappingDescriptor;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import com.cloudera.parserchains.queryservice.service.KafkaService;
import com.fasterxml.jackson.databind.ObjectReader;
import java.lang.reflect.Method;
import java.net.URI;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.adrianwalker.multilinestring.Multiline;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;

@SpringBootTest
@AutoConfigureMockMvc
public class InterconnectChainControllerTest {

  @Autowired
  private MockMvc mvc;
  @MockBean
  private KafkaService kafkaService;

  private static final String CHAIN_NAME_ONE = "chainName1";
  private static final String CHAIN_ID_ONE = "chainId1";
  private static final String PIPELINE_NAME = "pipelineName";
  private static final String CLUSTER_ID = "clusterId";

  private static int numFields = 0;

  @BeforeAll
  public static void beforeAll() {
    Method[] method = ParserChainSchema.class.getMethods();
    for (Method m : method) {
      if (m.getName().startsWith("set")) {
        numFields++;
      }
    }
  }

  @Test
  public void returns_list_of_parser_chains() throws Exception {
    List<ParserChainSummary> givenResultBody = Arrays.asList(
        new ParserChainSummary().setId("1").setName("chain1"),
        new ParserChainSummary().setId("2").setName("chain2"),
        new ParserChainSummary().setId("3").setName("chain3")
    );
    final ResponseEntity<Object> givenResult = ResponseEntity.ok(givenResultBody);
    final Map<String, Object> givenParams = new HashMap<>();
    givenParams.put(ApplicationConstants.PIPELINE_NAME_PARAM, PIPELINE_NAME);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.CHAIN_FIND_ALL), eq(CLUSTER_ID), eq(givenParams),
        ArgumentMatchers.any(ObjectReader.class)))
        .willReturn(givenResult);

    mvc.perform(MockMvcRequestBuilders.get(API_CHAINS_PROXY_URL)
            .param("pipelineName", PIPELINE_NAME)
            .param("clusterId", CLUSTER_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(jsonPath("$.*", instanceOf(List.class)))
        .andExpect(jsonPath("$.*", hasSize(3)))
        .andExpect(jsonPath("$.[0].id", is("1")))
        .andExpect(jsonPath("$.[0].name", is("chain1")))
        .andExpect(jsonPath("$.[1].id", is("2")))
        .andExpect(jsonPath("$.[1].name", is("chain2")))
        .andExpect(jsonPath("$.[2].id", is("3")))
        .andExpect(jsonPath("$.[2].name", is("chain3")));
  }

  @Test
  public void creates_parser_chain() throws Exception {
    String json = DefaultChainControllerTest.createChainJSON.replace("{name}", CHAIN_NAME_ONE);
    ParserChainSchema givenRequestBody = JSONUtils.INSTANCE.load(json, ParserChainSchema.class);
    ParserChainSchema givenResultBody = JSONUtils.INSTANCE.load(json, ParserChainSchema.class);
    givenResultBody.setId(CHAIN_ID_ONE);
    final URI resultUri = URI.create(API_CHAINS_READ_URL.replace("{id}", CHAIN_ID_ONE));

    final ResponseEntity<Object> givenResult = ResponseEntity.created(resultUri).body(givenResultBody);
    final Map<String, Object> givenParams = new HashMap<>();
    givenParams.put(ApplicationConstants.PIPELINE_NAME_PARAM, PIPELINE_NAME);
    givenParams.put(ApplicationConstants.CHAIN_PARAM, givenRequestBody);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.CHAIN_CREATE), eq(CLUSTER_ID), eq(givenParams),
        ArgumentMatchers.any(ObjectReader.class)))
        .willReturn(givenResult);

    mvc.perform(MockMvcRequestBuilders.post(API_CHAINS_PROXY_CREATE_URL)
            .param("pipelineName", PIPELINE_NAME)
            .param("clusterId", CLUSTER_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isCreated())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(
            header().string(HttpHeaders.LOCATION,
                API_CHAINS_READ_URL.replace("{id}", CHAIN_ID_ONE)))
        .andExpect(jsonPath("$.*", hasSize(numFields)))
        .andExpect(jsonPath("$.id", is(CHAIN_ID_ONE)))
        .andExpect(jsonPath("$.name", is(CHAIN_NAME_ONE)));
  }

  @Test
  public void read_chain_by_id_returns_chain_config() throws Exception {
    String json = DefaultChainControllerTest.readChainJSON.replace("{id}", CHAIN_ID_ONE).replace("{name}", CHAIN_NAME_ONE);
    final ParserChainSchema chain = JSONUtils.INSTANCE.load(json, ParserChainSchema.class);

    final ResponseEntity<Object> givenResult = ResponseEntity.ok(chain);
    final Map<String, Object> givenParams = new HashMap<>();
    givenParams.put(ApplicationConstants.PIPELINE_NAME_PARAM, PIPELINE_NAME);
    givenParams.put(ApplicationConstants.ID_PARAM, CHAIN_ID_ONE);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.CHAIN_READ), eq(CLUSTER_ID), eq(givenParams),
        ArgumentMatchers.any(ObjectReader.class)))
        .willReturn(givenResult);

    mvc.perform(
            MockMvcRequestBuilders
                .get(API_CHAINS_PROXY_READ_URL.replace("{id}", CHAIN_ID_ONE))
                .param("pipelineName", PIPELINE_NAME)
                .param("clusterId", CLUSTER_ID)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(jsonPath("$.id", is(CHAIN_ID_ONE)))
        .andExpect(jsonPath("$.name", is(CHAIN_NAME_ONE)));
  }

  @Test
  public void update_chain_by_id_returns_updated_chain_config() throws Exception {
    String updateJson = DefaultChainControllerTest.readChainJSON.replace("{id}", CHAIN_ID_ONE).replace("{name}",
        CHAIN_NAME_ONE);
    final ParserChainSchema updatedChain = JSONUtils.INSTANCE.load(updateJson, ParserChainSchema.class);

    final ResponseEntity<Object> givenResult = ResponseEntity.noContent().build();
    final Map<String, Object> givenParams = new HashMap<>();
    givenParams.put(ApplicationConstants.PIPELINE_NAME_PARAM, PIPELINE_NAME);
    givenParams.put(ApplicationConstants.CHAIN_PARAM, updatedChain);
    givenParams.put(ApplicationConstants.ID_PARAM, CHAIN_ID_ONE);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.CHAIN_UPDATE), eq(CLUSTER_ID), eq(givenParams),
        ArgumentMatchers.any(ObjectReader.class)))
        .willReturn(givenResult);

    mvc.perform(MockMvcRequestBuilders
            .put(API_CHAINS_PROXY_UPDATE_URL.replace("{id}", CHAIN_ID_ONE))
            .param("pipelineName", PIPELINE_NAME)
            .param("clusterId", CLUSTER_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(updateJson))
        .andExpect(status().isNoContent());
  }

  @Test
  public void deleting_existing_chain_succeeds() throws Exception {
    final ResponseEntity<Object> givenResult = ResponseEntity.noContent().build();

    final Map<String, Object> givenParams = new HashMap<>();
    givenParams.put(ApplicationConstants.PIPELINE_NAME_PARAM, PIPELINE_NAME);
    givenParams.put(ApplicationConstants.ID_PARAM, CHAIN_ID_ONE);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.CHAIN_DELETE), eq(CLUSTER_ID), eq(givenParams),
        isNull()))
        .willReturn(givenResult);

    mvc.perform(
            MockMvcRequestBuilders
                .delete(API_CHAINS_PROXY_DELETE_URL.replace("{id}", CHAIN_ID_ONE))
                .param("pipelineName", PIPELINE_NAME)
                .param("clusterId", CLUSTER_ID)
                .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isNoContent());
  }

  /**
   * { "filePath" : "{path}" }
   */
  @Multiline
  public static String indexingDescriptorJson;

  @Test
  public void getting_mappings_from_path() throws Exception {
    String json = indexingDescriptorJson.replace("{path}", CHAIN_NAME_ONE);
    IndexMappingDescriptor givenRequestBody = JSONUtils.INSTANCE.load(json, IndexMappingDescriptor.class);
    final Map<String, Object> givenResultBody = new HashMap<>();
    givenResultBody.put("filePath", CHAIN_NAME_ONE);

    final ResponseEntity<Object> givenResult = ResponseEntity.ok(givenResultBody);
    final Map<String, Object> givenParams = new HashMap<>();
    givenParams.put(ApplicationConstants.PIPELINE_NAME_PARAM, PIPELINE_NAME);
    givenParams.put(ApplicationConstants.BODY_PARAM, givenRequestBody);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.CHAIN_INDEXING_GET), eq(CLUSTER_ID), eq(givenParams),
        ArgumentMatchers.any(ObjectReader.class)))
        .willReturn(givenResult);

    mvc.perform(MockMvcRequestBuilders.post(API_INDEXING_PROXY_URL)
            .param("pipelineName", PIPELINE_NAME)
            .param("clusterId", CLUSTER_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(jsonPath("$.*", hasSize(1)))
        .andExpect(jsonPath("$.filePath", is(CHAIN_NAME_ONE)));
  }

  @Test
  public void should_run_test() throws Exception {
    String requestJson = DefaultChainControllerTest.test_chain_request;
    String responseJson = DefaultChainControllerTest.test_chain_response;

    ChainTestRequest givenRequestBody = JSONUtils.INSTANCE.load(requestJson, ChainTestRequest.class);
    ChainTestResponse givenResultBody = JSONUtils.INSTANCE.load(responseJson, ChainTestResponse.class);

    final ResponseEntity<Object> givenResult = ResponseEntity.ok(givenResultBody);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.CHAIN_TEST), eq(CLUSTER_ID), anyMap(),
        ArgumentMatchers.any(ObjectReader.class)))
        .willAnswer(invocation -> {
          final Map<String, Object> paramsArg = invocation.getArgument(2);
          final ChainTestRequest actualRequestBody = (ChainTestRequest) paramsArg.get("testRun");

          assertThat(actualRequestBody.getParserChainSchema(), samePropertyValuesAs(givenRequestBody.getParserChainSchema()));

          return givenResult;
        });

    final String response = mvc.perform(MockMvcRequestBuilders.post(API_PARSER_PROXY_TEST_URL)
            .param("pipelineName", PIPELINE_NAME)
            .param("clusterId", CLUSTER_ID)
            .accept(MediaType.APPLICATION_JSON)
            .contentType(MediaType.APPLICATION_JSON)
            .content(requestJson))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andReturn()
        .getResponse()
        .getContentAsString();
    ChainTestResponse actual = JSONUtils.INSTANCE.load(response, ChainTestResponse.class);
    assertThat(actual, is(givenResultBody));
  }

}
