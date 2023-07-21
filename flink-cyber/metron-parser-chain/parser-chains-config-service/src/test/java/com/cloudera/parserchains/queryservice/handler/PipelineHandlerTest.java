package com.cloudera.parserchains.queryservice.handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.cloudera.parserchains.queryservice.controller.impl.DefaultPipelineController;
import java.util.HashSet;
import java.util.Set;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

public class PipelineHandlerTest {

  private DefaultPipelineController pipelineController;
  private PipelineHandler pipelineHandler;

  @BeforeEach
  void beforeEach() {
    pipelineController = Mockito.mock(DefaultPipelineController.class);

    pipelineHandler = new PipelineHandler(pipelineController);
  }

  @Test
  @SneakyThrows
  public void shouldFindAll(){
    //Given
    final HashSet<String> body = new HashSet<>();
    body.add("test");
    body.add("value");
    final ResponseEntity<Set<String>> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

    //When
    when(pipelineController.findAll(null)).thenReturn(responseEntity);
    final ResponseEntity<?> result = pipelineHandler.findAll();

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(pipelineController, times(1)).findAll(null);
  }

}