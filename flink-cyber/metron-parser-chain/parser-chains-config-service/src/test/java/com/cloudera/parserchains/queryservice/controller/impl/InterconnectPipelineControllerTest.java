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

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PIPELINE_PROXY_BASE_URL;
import static org.hamcrest.CoreMatchers.instanceOf;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import com.cloudera.parserchains.queryservice.service.KafkaService;
import com.fasterxml.jackson.databind.ObjectReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.hamcrest.Matchers;
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
public class InterconnectPipelineControllerTest {

  @Autowired
  private MockMvc mvc;
  @MockBean
  private KafkaService kafkaService;

  private static final String CLUSTER_ID = "clusterId";

  @Test
  public void returns_list_of_all_pipelines() throws Exception {
    final Set<String> givenResultBody = new HashSet<>();
    givenResultBody.add("test");
    givenResultBody.add("test2");
    final ResponseEntity<Object> givenResult = ResponseEntity.ok(givenResultBody);

    given(kafkaService.sendWithReply(eq(KafkaMessageType.PIPELINES_FIND_ALL), eq(CLUSTER_ID), isNull(),
            ArgumentMatchers.any(ObjectReader.class)))
        .willReturn(givenResult);
    mvc.perform(MockMvcRequestBuilders.get(PIPELINE_PROXY_BASE_URL)
            .param("clusterId", CLUSTER_ID)
            .accept(MediaType.APPLICATION_JSON))
        .andExpect(status().isOk())
        .andExpect(header().string(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE))
        .andExpect(jsonPath("$.*", instanceOf(List.class)))
        .andExpect(jsonPath("$.*", hasSize(2)))
        .andExpect(jsonPath("$.[0]", Matchers.anyOf(is("test"), is("test2"))))
        .andExpect(jsonPath("$.[1]", Matchers.anyOf(is("test"), is("test2"))));
  }

}
