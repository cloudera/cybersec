package com.cloudera.parserchains.queryservice.model.enums;

import java.util.Arrays;

public enum KafkaMessageType {
  //ChainController messages
  CHAIN_FIND_ALL,
  CHAIN_CREATE,
  CHAIN_READ,
  CHAIN_UPDATE,
  CHAIN_DELETE,
  CHAIN_TEST,

  //ParserController messages
  PARSER_FIND_ALL,
  PARSER_DESCRIBE_ALL,

  //PipelineController
  PIPELINES_FIND_ALL,

  UNKNOWN;

  public static KafkaMessageType fromString(String name){
    if (name == null){
      return UNKNOWN;
    }
    return Arrays.stream(KafkaMessageType.values())
        .filter(value -> value.name().equals(name))
        .findFirst()
        .orElse(UNKNOWN);
  }
}
