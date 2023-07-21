package com.cloudera.parserchains.queryservice.handler;

import com.cloudera.parserchains.queryservice.model.ThrowingRunnable;
import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import java.util.stream.Stream;
import lombok.SneakyThrows;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;

public class InterconnectHandlerTest {

  private static final String TEST_VALUE = "testValue";
  private static PipelineHandler pipelineHandler;
  private static ParserHandler parserHandler;
  private static ChainHandler chainHandler;

  private InterconnectHandler interconnectHandler;

  @BeforeEach
  void beforeEach() {
    pipelineHandler = Mockito.mock(PipelineHandler.class);
    parserHandler = Mockito.mock(ParserHandler.class);
    chainHandler = Mockito.mock(ChainHandler.class);

    interconnectHandler = new InterconnectHandler(pipelineHandler, parserHandler, chainHandler);
  }

  @SneakyThrows
  @ParameterizedTest
  @MethodSource("testData")
  public void testDifferentHandlerValues(String key, String value, ThrowingRunnable assertFunc) {
    interconnectHandler.handle(key, value);
    assertFunc.run();
  }

  static Stream<Arguments> testData() {
    return Stream.of(
        Arguments.of(KafkaMessageType.CHAIN_FIND_ALL.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(chainHandler, Mockito.times(1)).findAll(TEST_VALUE)),
        Arguments.of(KafkaMessageType.CHAIN_CREATE.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(chainHandler, Mockito.times(1)).create(TEST_VALUE)),
        Arguments.of(KafkaMessageType.CHAIN_READ.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(chainHandler, Mockito.times(1)).read(TEST_VALUE)),
        Arguments.of(KafkaMessageType.CHAIN_UPDATE.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(chainHandler, Mockito.times(1)).update(TEST_VALUE)),
        Arguments.of(KafkaMessageType.CHAIN_DELETE.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(chainHandler, Mockito.times(1)).delete(TEST_VALUE)),
        Arguments.of(KafkaMessageType.CHAIN_INDEXING_GET.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(chainHandler, Mockito.times(1)).getMappingsFromPath(TEST_VALUE)),
        Arguments.of(KafkaMessageType.CHAIN_TEST.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(chainHandler, Mockito.times(1)).test(TEST_VALUE)),

        Arguments.of(KafkaMessageType.PARSER_FIND_ALL.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(parserHandler, Mockito.times(1)).findAll()),
        Arguments.of(KafkaMessageType.PARSER_DESCRIBE_ALL.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(parserHandler, Mockito.times(1)).describeAll()),

        Arguments.of(KafkaMessageType.PIPELINES_FIND_ALL.name(), TEST_VALUE,
            (ThrowingRunnable) () -> Mockito.verify(pipelineHandler, Mockito.times(1)).findAll())
    );
  }


}