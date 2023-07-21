package com.cloudera.parserchains.queryservice.handler;

import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.BODY_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.CHAIN_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.ID_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.PIPELINE_NAME_PARAM;
import static com.cloudera.parserchains.queryservice.common.ApplicationConstants.TEST_RUN_PARAM;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.samePropertyValuesAs;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.cloudera.parserchains.core.model.define.ParserChainSchema;
import com.cloudera.parserchains.core.utils.JSONUtils;
import com.cloudera.parserchains.queryservice.controller.impl.DefaultChainController;
import com.cloudera.parserchains.queryservice.model.describe.IndexMappingDescriptor;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestRequest;
import com.cloudera.parserchains.queryservice.model.exec.ChainTestResponse;
import com.cloudera.parserchains.queryservice.model.exec.ParserResult;
import com.cloudera.parserchains.queryservice.model.exec.SampleData;
import com.cloudera.parserchains.queryservice.model.summary.ParserChainSummary;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class ChainHandlerTest {

  private DefaultChainController chainController;
  private ChainHandler chainHandler;
  private final String givenPipelineName = "pipelineName";

  @BeforeEach
  void beforeEach() {
    chainController = Mockito.mock(DefaultChainController.class);

    chainHandler = new ChainHandler(chainController);
  }

  @Test
  @SneakyThrows
  public void shouldFindAll() {
    //Given
    final List<ParserChainSummary> body = new ArrayList<>();
    body.add(new ParserChainSummary().setName("test"));
    body.add(new ParserChainSummary().setName("name"));
    final ResponseEntity<List<ParserChainSummary>> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

    final Map<String, Object> params = new HashMap<>();
    params.put(PIPELINE_NAME_PARAM, givenPipelineName);

    final String paramsJson = JSONUtils.INSTANCE.toJSON(params, false);

    //When
    when(chainController.findAll(givenPipelineName, null)).thenReturn(responseEntity);
    final ResponseEntity<?> result = chainHandler.findAll(paramsJson);

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(chainController, times(1)).findAll(givenPipelineName, null);
  }

  @Test
  @SneakyThrows
  public void shouldCreate() {
    //Given
    final ParserChainSchema body = new ParserChainSchema().setName("testName");
    final ResponseEntity<ParserChainSchema> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

    final Map<String, Object> params = new HashMap<>();
    params.put(PIPELINE_NAME_PARAM, givenPipelineName);
    params.put(CHAIN_PARAM, body);

    final String paramsJson = JSONUtils.INSTANCE.toJSON(params, false);

    //When
    when(chainController.create(givenPipelineName, null, body)).thenReturn(responseEntity);
    final ResponseEntity<?> result = chainHandler.create(paramsJson);

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(chainController, times(1)).create(givenPipelineName, null, body);
  }

  @Test
  @SneakyThrows
  public void shouldRead() {
    //Given
    final ParserChainSchema body = new ParserChainSchema().setName("testName");
    final ResponseEntity<ParserChainSchema> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);
    final String givenId = "id";

    final Map<String, Object> params = new HashMap<>();
    params.put(PIPELINE_NAME_PARAM, givenPipelineName);
    params.put(CHAIN_PARAM, body);
    params.put(ID_PARAM, givenId);

    final String paramsJson = JSONUtils.INSTANCE.toJSON(params, false);

    //When
    when(chainController.read(givenPipelineName, null, givenId)).thenReturn(responseEntity);
    final ResponseEntity<?> result = chainHandler.read(paramsJson);

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(chainController, times(1)).read(givenPipelineName, null, givenId);
  }

  @Test
  @SneakyThrows
  public void shouldUpdate() {
    //Given
    final ParserChainSchema body = new ParserChainSchema().setName("testName");
    final ResponseEntity<ParserChainSchema> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);
    final String givenId = "id";

    final Map<String, Object> params = new HashMap<>();
    params.put(PIPELINE_NAME_PARAM, givenPipelineName);
    params.put(CHAIN_PARAM, body);
    params.put(ID_PARAM, givenId);

    final String paramsJson = JSONUtils.INSTANCE.toJSON(params, false);

    //When
    when(chainController.update(givenPipelineName, null, body, givenId)).thenReturn(responseEntity);
    final ResponseEntity<?> result = chainHandler.update(paramsJson);

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(chainController, times(1)).update(givenPipelineName, null, body, givenId);
  }

  @Test
  @SneakyThrows
  public void shouldDelete() {
    //Given
    final ResponseEntity<Void> responseEntity = new ResponseEntity<>(HttpStatus.OK);
    final String givenId = "id";

    final Map<String, Object> params = new HashMap<>();
    params.put(PIPELINE_NAME_PARAM, givenPipelineName);
    params.put(ID_PARAM, givenId);

    final String paramsJson = JSONUtils.INSTANCE.toJSON(params, false);

    //When
    when(chainController.delete(givenPipelineName, null, givenId)).thenReturn(responseEntity);
    final ResponseEntity<?> result = chainHandler.delete(paramsJson);

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(chainController, times(1)).delete(givenPipelineName, null, givenId);
  }

  @Test
  @SneakyThrows
  public void shouldGetMappingsFromPath() {
    //Given
    final Map<String, Object> body = new HashMap<>();
    body.put("test", "value");
    final ResponseEntity<Map<String, Object>> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);
    final IndexMappingDescriptor requestBody = new IndexMappingDescriptor();

    final Map<String, Object> params = new HashMap<>();
    params.put(PIPELINE_NAME_PARAM, givenPipelineName);
    params.put(BODY_PARAM, requestBody);

    final String paramsJson = JSONUtils.INSTANCE.toJSON(params, false);

    //When
    when(chainController.getMappingsFromPath(givenPipelineName, null, requestBody)).thenReturn(responseEntity);
    final ResponseEntity<?> result = chainHandler.getMappingsFromPath(paramsJson);

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(chainController, times(1)).getMappingsFromPath(givenPipelineName, null, requestBody);
  }

  @Test
  @SneakyThrows
  public void shouldTest() {
    //Given
    final ChainTestResponse body = new ChainTestResponse().addResult(new ParserResult().addInput("field", "value"));
    final ResponseEntity<ChainTestResponse> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);
    final ChainTestRequest requestBody = new ChainTestRequest()
        .setSampleData(new SampleData().setType("testType"))
        .setParserChainSchema(new ParserChainSchema().setName("testName"));

    final Map<String, Object> params = new HashMap<>();
    params.put(TEST_RUN_PARAM, requestBody);

    final String paramsJson = JSONUtils.INSTANCE.toJSON(params, false);

    //When
    when(chainController.test(isNull(), any(ChainTestRequest.class))).thenAnswer(invocation -> {
      final ChainTestRequest argument = invocation.getArgument(1);
      assertThat(argument.getParserChainSchema(), samePropertyValuesAs(requestBody.getParserChainSchema()));
      assertThat(argument.getSampleData(), samePropertyValuesAs(requestBody.getSampleData()));
      return responseEntity;
    });
    final ResponseEntity<?> result = chainHandler.test(paramsJson);

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(chainController, times(1)).test(isNull(), any(ChainTestRequest.class));
  }

}