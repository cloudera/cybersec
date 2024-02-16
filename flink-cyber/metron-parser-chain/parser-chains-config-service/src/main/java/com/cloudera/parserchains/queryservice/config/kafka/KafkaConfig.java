package com.cloudera.parserchains.queryservice.config.kafka;

import com.cloudera.service.common.config.kafka.ClouderaKafkaProperties;
import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.response.ResponseBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableBeanFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Scope;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.GenericMessageListenerContainer;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
@EnableConfigurationProperties({ClouderaKafkaProperties.class})
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
   * @return Map with key of clusterId and value - kafkaProperties. This map is a mapping between clusterId and connection details for that cluster
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
   * @return Map with key of clusterId and value - kafkaTemplate that allows to send requests over Kafka to different clusters and get
   * responses.
   */
  @Bean(name = "kafkaTemplatePool")
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public Map<String, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody>> kafkaTemplatePool(
      @Qualifier("kafka-external-cluster-map") Map<String, ClouderaKafkaProperties> replyKafkaPropertiesMap) {
    final Map<String, ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody>> templatePool = new HashMap<>();

    replyKafkaPropertiesMap.forEach((clusterId, kafkaProperties) -> {
      final ProducerFactory<String, RequestBody> producerFactory = producerFactory(kafkaProperties);
      final ConsumerFactory<String, ResponseBody> consumerFactory = consumerFactory(kafkaProperties);
      final GenericMessageListenerContainer<String, ResponseBody> replyContainer = replyContainer(consumerFactory,
          kafkaProperties.getReplyTopic());

      final ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody> kafkaTemplate = replyingKafkaTemplate(producerFactory,
          replyContainer, kafkaProperties.getRequestTopic());
      kafkaTemplate.start();

      templatePool.put(clusterId, kafkaTemplate);
    });

    return Collections.unmodifiableMap(templatePool);
  }

  @Bean
  @Scope(ConfigurableBeanFactory.SCOPE_SINGLETON)
  public Set<String> kafkaClustersSet(@Qualifier("kafka-external-cluster-map") Map<String, ClouderaKafkaProperties> replyKafkaPropertiesMap) {
    return Collections.unmodifiableSet(replyKafkaPropertiesMap.keySet());
  }


  private ProducerFactory<String, RequestBody> producerFactory(ClouderaKafkaProperties kafkaProperties) {
    return new DefaultKafkaProducerFactory<>(kafkaProperties.buildConsumerProperties(), new StringSerializer(), new JsonSerializer<>());
  }

  private ConsumerFactory<String, ResponseBody> consumerFactory(ClouderaKafkaProperties kafkaProperties) {
    return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), new JsonDeserializer<>(ResponseBody.class));
  }

  private static ClouderaReplyingKafkaTemplate<String, RequestBody, ResponseBody> replyingKafkaTemplate(
      ProducerFactory<String, RequestBody> producerFactory, GenericMessageListenerContainer<String, ResponseBody> replyContainer,
      String requestTopic) {
    return new ClouderaReplyingKafkaTemplate<>(producerFactory, replyContainer, requestTopic);
  }

  private static GenericMessageListenerContainer<String, ResponseBody> replyContainer(
      ConsumerFactory<String, ResponseBody> consumerFactory, String replyTopic) {
    ContainerProperties containerProperties = new ContainerProperties(replyTopic);
    return new ConcurrentMessageListenerContainer<>(consumerFactory, containerProperties);
  }

}
