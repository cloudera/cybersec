package com.cloudera.parserchains.queryservice.handler;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import com.cloudera.parserchains.core.model.define.ParserID;
import com.cloudera.parserchains.core.model.define.ParserName;
import com.cloudera.parserchains.queryservice.controller.impl.DefaultParserController;
import com.cloudera.parserchains.queryservice.model.describe.ParserDescriptor;
import com.cloudera.parserchains.queryservice.model.summary.ParserSummary;
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

public class ParserHandlerTest {

  private DefaultParserController parserController;
  private ParserHandler parserHandler;

  @BeforeEach
  void beforeEach() {
    parserController = Mockito.mock(DefaultParserController.class);

    parserHandler = new ParserHandler(parserController);
  }

  @Test
  @SneakyThrows
  public void shouldFindAll(){
    //Given
    final List<ParserSummary> body = new ArrayList<>();
    body.add(new ParserSummary().setName("test"));
    body.add(new ParserSummary().setName("name"));
    final ResponseEntity<List<ParserSummary>> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

    //When
    when(parserController.findAll(null)).thenReturn(responseEntity);
    final ResponseEntity<?> result = parserHandler.findAll();

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(parserController, times(1)).findAll(null);
  }

  @Test
  @SneakyThrows
  public void shouldDescribeAll(){
    //Given
    final Map<ParserID, ParserDescriptor> body = new HashMap<>();
    body.put(ParserID.of(String.class), new ParserDescriptor().setParserName(ParserName.of("test")));
    body.put(ParserID.of(String.class), new ParserDescriptor().setParserName(ParserName.of("name")));
    final ResponseEntity<Map<ParserID, ParserDescriptor>> responseEntity = new ResponseEntity<>(body, HttpStatus.OK);

    //When
    when(parserController.describeAll(null)).thenReturn(responseEntity);
    final ResponseEntity<?> result = parserHandler.describeAll();

    //Then
    assertThat(result, equalTo(responseEntity));
    verify(parserController, times(1)).describeAll(null);
  }

}