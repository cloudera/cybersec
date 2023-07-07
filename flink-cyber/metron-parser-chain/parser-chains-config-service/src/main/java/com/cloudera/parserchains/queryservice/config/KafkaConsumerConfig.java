package com.cloudera.parserchains.queryservice.config;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.TopicBuilder;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListenerContainer;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConsumerConfig {

  @Value("${spring.kafka.consumer.reply-topic}")
  private String replyTopic;

  @Value("${spring.kafka.producer.request-topic}")
  private String requestTopic;

  @Bean
  public ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
  }

  @Bean
  public ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {
    return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
  }

  @Bean
  public ConcurrentKafkaListenerContainerFactory<String, String> kafkaListenerContainerFactory(
      ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate,
      ConsumerFactory<String, String> consumerFactory) {
    ConcurrentKafkaListenerContainerFactory<String, String> factory = new ConcurrentKafkaListenerContainerFactory<>();
    factory.setConsumerFactory(consumerFactory);
    factory.setReplyTemplate(replyingKafkaTemplate);
    return factory;
  }

  @Bean
  public ReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate(
      ProducerFactory<String, String> producerFactory, GenericMessageListenerContainer<String, String> replyContainer) {
    return new ReplyingKafkaTemplate<>(producerFactory, replyContainer);
  }

  @Bean
  public KafkaMessageListenerContainer<String, String> replyContainer(ConsumerFactory<String, String> consumerFactory) {
    ContainerProperties containerProperties = new ContainerProperties(replyTopic);
    return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
  }

  public String getReplyTopic() {
    return replyTopic;
  }

  public String getRequestTopic() {
    return requestTopic;
  }
}
