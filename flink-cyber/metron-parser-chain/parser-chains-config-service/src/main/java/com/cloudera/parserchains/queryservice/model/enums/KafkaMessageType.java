package com.cloudera.parserchains.queryservice.model.enums;

import java.util.Arrays;

public enum KafkaMessageType {
  PARSER, CLUSTER, UNKNOWN;

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
