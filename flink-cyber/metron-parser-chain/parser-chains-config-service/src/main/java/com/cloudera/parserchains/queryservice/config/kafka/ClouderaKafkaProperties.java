package com.cloudera.parserchains.queryservice.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;

@Getter
@Setter
public class ClouderaKafkaProperties extends KafkaProperties {

  private String replyTopic;

  private String requestTopic;

}
