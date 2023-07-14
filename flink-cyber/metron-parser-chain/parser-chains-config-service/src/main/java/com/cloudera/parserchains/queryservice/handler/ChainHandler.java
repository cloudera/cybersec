package com.cloudera.parserchains.queryservice.handler;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.BODY_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PIPELINE_NAME_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.TEST_RUN_PARAM;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.common.ApplicationConstants;
import com.cloudera.parserchains.queryservice.controller.impl.DefaultChainController;
import com.cloudera.parserchains.queryservice.model.describe.IndexMappingDescriptor;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectReader;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ChainHandler {

  private static final ObjectMapper MAPPER = JSONUtils.INSTANCE.getMapper();
  private static final ObjectReader CHAIN_TEST_READER = MAPPER.readerFor(ChainTestRequest.class);
  private static final ObjectReader CHAIN_SCHEMA_READER = MAPPER.readerFor(ParserChainSchema.class);
  private static final ObjectReader INDEX_MAPPING_READER = MAPPER.readerFor(IndexMappingDescriptor.class);

  private final DefaultChainController chainController;

  public ResponseEntity<?> findAll(String body) throws IOException {
    final JsonNode params = MAPPER.readTree(body);
    final String pipelineName = getPipelineName(params);

    return chainController.findAll(pipelineName, null);
  }

  public ResponseEntity<?> create(String body) throws IOException {
    final JsonNode params = MAPPER.readTree(body);
    final String pipelineName = getPipelineName(params);
    final ParserChainSchema chain = CHAIN_SCHEMA_READER.readValue(params.get(PIPELINE_NAME_PARAM));

    return chainController.create(pipelineName, null, chain);
  }

  public ResponseEntity<?> read(String body) throws IOException {
    final JsonNode params = MAPPER.readTree(body);
    final String pipelineName = getPipelineName(params);
    final String id = params.get(ApplicationConstants.ID_PARAM).asText();

    return chainController.read(pipelineName, null, id);
  }

  public ResponseEntity<?> update(String body) throws IOException {
    final JsonNode params = MAPPER.readTree(body);
    final String pipelineName = getPipelineName(params);
    final ParserChainSchema chain = CHAIN_SCHEMA_READER.readValue(params.get(PIPELINE_NAME_PARAM));
    final String id = params.get(ApplicationConstants.ID_PARAM).asText();

    return chainController.update(pipelineName, null, chain, id);
  }

  public ResponseEntity<?> delete(String body) throws IOException {
    final JsonNode params = MAPPER.readTree(body);
    final String pipelineName = getPipelineName(params);
    final String id = params.get(ApplicationConstants.ID_PARAM).asText();

    return chainController.delete(pipelineName, null, id);
  }

  public ResponseEntity<?> getMappingsFromPath(String body) throws IOException {
    final JsonNode params = MAPPER.readTree(body);
    final String pipelineName = getPipelineName(params);
    final IndexMappingDescriptor descriptor = INDEX_MAPPING_READER.readValue(params.get(BODY_PARAM));

    return chainController.getMappingsFromPath(pipelineName, null, descriptor);
  }

  public ResponseEntity<?> test(String body) throws IOException {
    final JsonNode params = MAPPER.readTree(body);
    final String pipelineName = getPipelineName(params);
    final ChainTestRequest testRun = CHAIN_TEST_READER.readValue(params.get(TEST_RUN_PARAM));

    return chainController.test(pipelineName, testRun);
  }

  private static String getPipelineName(JsonNode params){
    final JsonNode pipelineNameParam = params.get(PIPELINE_NAME_PARAM);
    if (pipelineNameParam == null){
      return null;
    }
    return pipelineNameParam.asText();
  }

}
