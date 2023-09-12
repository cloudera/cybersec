package com.cloudera.service.common.config.kafka;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties
public class ClouderaKafkaProperties extends KafkaProperties {

  private String replyTopic;

  private String requestTopic;

}
