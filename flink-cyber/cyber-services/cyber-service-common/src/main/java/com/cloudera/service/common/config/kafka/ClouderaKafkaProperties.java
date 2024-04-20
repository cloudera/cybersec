package com.cloudera.service.common.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
public class ClouderaKafkaProperties extends KafkaProperties {
  private String replyTopic;
  private String requestTopic;
}
