package com.cloudera.parserchains.queryservice.config.kafka;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.metron.stellar.dsl.functions.HashFunctions.Hash;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListenerContainer;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {

  /***
   * Provides the default kafka properties for the consumers
   * @return Default kafka properties for the consumers
   */
  @Bean
  @Primary
  public ClouderaKafkaProperties kafkaProperties() {
    return new ClouderaKafkaProperties();
  }

  /***
   * Provides a map with key=clusterId and value=ClouderaKafkaProperties
   * @return Map<clusterId, kafkaProperties> which is a mapping between clusterId and connection details for that cluster
   */
  @Bean(name = "kafka-external-cluster-map")
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  @ConfigurationProperties("spring.kafka.external-clusters")
  public Map<String, ClouderaKafkaProperties> replyKafkaPropertiesMap() {
    return new HashMap<>();
  }

  /**
   * Map of KafkaTemplates that with key=clusterId, which allows to send requests over Kafka to different clusters and
   * get responses.
   *
   * @param replyKafkaPropertiesMap - replyKafkaPropertiesMap bean with properties for each cluster, injected by Spring
   * @return Map<clusterId, kafkaTemplate> that allows to send requests over Kafka to different clusters and get
   * responses.
   */
  @Bean(name = "kafkaTemplatePool")
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public Map<String, ClouderaReplyingKafkaTemplate<String, String, String>> kafkaTemplatePool(
      @Qualifier("kafka-external-cluster-map") Map<String, ClouderaKafkaProperties> replyKafkaPropertiesMap) {
    final Map<String, ClouderaReplyingKafkaTemplate<String, String, String>> templatePool = new HashMap<>();

    replyKafkaPropertiesMap.forEach((clusterId, kafkaProperties) -> {
      final ProducerFactory<String, String> producerFactory = producerFactory(kafkaProperties);
      final ConsumerFactory<String, String> consumerFactory = consumerFactory(kafkaProperties);
      final KafkaMessageListenerContainer<String, String> replyContainer = replyContainer(consumerFactory,
          kafkaProperties.getReplyTopic());

      final ClouderaReplyingKafkaTemplate<String, String, String> kafkaTemplate = replyingKafkaTemplate(producerFactory,
          replyContainer, kafkaProperties.getRequestTopic());
      kafkaTemplate.start();

      templatePool.put(clusterId, kafkaTemplate);
    });

    return Collections.unmodifiableMap(templatePool);
  }

  private static ProducerFactory<String, String> producerFactory(KafkaProperties kafkaProperties) {
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildProducerProperties());
  }

  private static ConsumerFactory<String, String> consumerFactory(KafkaProperties kafkaProperties) {
    return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties());
  }

  private static ClouderaReplyingKafkaTemplate<String, String, String> replyingKafkaTemplate(
      ProducerFactory<String, String> producerFactory, GenericMessageListenerContainer<String, String> replyContainer,
      String requestTopic) {
    return new ClouderaReplyingKafkaTemplate<>(producerFactory, replyContainer, requestTopic);
  }

  private static KafkaMessageListenerContainer<String, String> replyContainer(
      ConsumerFactory<String, String> consumerFactory, String replyTopic) {
    ContainerProperties containerProperties = new ContainerProperties(replyTopic);
    return new KafkaMessageListenerContainer<>(consumerFactory, containerProperties);
  }

}
