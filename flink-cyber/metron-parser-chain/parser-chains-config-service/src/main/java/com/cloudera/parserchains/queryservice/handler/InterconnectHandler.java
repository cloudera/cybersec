package com.cloudera.parserchains.queryservice.handler;

import com.cloudera.parserchains.queryservice.model.enums.KafkaMessageType;
import org.springframework.stereotype.Service;

@Service
public class InterconnectHandler {

  public String handle(String key, String value) {
    switch (KafkaMessageType.fromString(key)) {
      case PARSER:
        return "Processed parser request: " + value;
      case CLUSTER:
        return "Processed cluster request: " + value;
      default:
        return String.format("Processed unknown request (%s): %s", key, value);
    }
  }
}
