package com.cloudera.cyber.restcli.configuration;

import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.response.ResponseBody;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Slf4j
@EnableKafka
@Configuration
@AllArgsConstructor
@EnableConfigurationProperties({KafkaProperties.class})
public class KafkaConfig {

    public final KafkaProperties kafkaProperties;

    @Bean
    public ProducerFactory<String, ResponseBody> kafkaProducerFactory() {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildConsumerProperties(), new StringSerializer(), new JsonSerializer<>());
    }

    @Bean
    public ConsumerFactory<String, RequestBody> kafkaConsumerFactory() {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), new JsonDeserializer<>(RequestBody.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RequestBody> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<String, RequestBody> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(kafkaConsumerFactory());
        factory.setReplyTemplate(kafkaTemplate());
        return factory;
    }

    @Bean
    public KafkaTemplate<String, ResponseBody> kafkaTemplate() {
        return new KafkaTemplate<>(kafkaProducerFactory());
    }
}
