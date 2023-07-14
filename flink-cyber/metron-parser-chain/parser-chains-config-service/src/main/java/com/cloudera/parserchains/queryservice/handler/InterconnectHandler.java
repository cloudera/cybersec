package com.cloudera.parserchains.queryservice.handler;

import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class InterconnectHandler {

  private final PipelineHandler pipelineHandler;
  private final ParserHandler parserHandler;
  private final ChainHandler chainHandler;

  public ResponseEntity<?> handle(String key, String value) throws IOException {
    switch (KafkaMessageType.fromString(key)) {
      case CHAIN_FIND_ALL:
        return chainHandler.findAll(value);
      case CHAIN_CREATE:
        return chainHandler.create(value);
      case CHAIN_READ:
        return chainHandler.read(value);
      case CHAIN_UPDATE:
        return chainHandler.update(value);
      case CHAIN_DELETE:
        return chainHandler.delete(value);
      case CHAIN_INDEXING_GET:
        return chainHandler.getMappingsFromPath(value);
      case CHAIN_TEST:
        return chainHandler.test(value);

      case PARSER_FIND_ALL:
        return parserHandler.findAll();
      case PARSER_DESCRIBE_ALL:
        return parserHandler.describeAll();

      case PIPELINES_FIND_ALL:
        return pipelineHandler.findAll();

      default:
        throw new IOException(String.format("Unknown Kafka message type [%s]!", key));
    }
  }
}
