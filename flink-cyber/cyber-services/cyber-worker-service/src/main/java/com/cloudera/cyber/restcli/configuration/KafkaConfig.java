package com.cloudera.cyber.restcli.configuration;

import com.cloudera.service.common.request.RequestBody;
import com.cloudera.service.common.response.ResponseBody;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.springframework.boot.autoconfigure.kafka.KafkaProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.annotation.EnableKafka;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.core.ConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaConsumerFactory;
import org.springframework.kafka.core.DefaultKafkaProducerFactory;
import org.springframework.kafka.core.ProducerFactory;
import org.springframework.kafka.support.serializer.JsonDeserializer;
import org.springframework.kafka.support.serializer.JsonSerializer;

@Slf4j
@EnableKafka
@Configuration
@RequiredArgsConstructor
public class KafkaConfig {
    @Bean
    private static ProducerFactory<String, ResponseBody> producerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaProducerFactory<>(kafkaProperties.buildConsumerProperties(), new StringSerializer(), new JsonSerializer<>());
    }

    @Bean
    private static ConsumerFactory<String, RequestBody> consumerFactory(KafkaProperties kafkaProperties) {
        return new DefaultKafkaConsumerFactory<>(kafkaProperties.buildConsumerProperties(), new StringDeserializer(), new JsonDeserializer<>(RequestBody.class));
    }

    @Bean
    public ConcurrentKafkaListenerContainerFactory<String, RequestBody> kafkaListenerContainerFactory(ConsumerFactory<String, RequestBody> cf) {
        ConcurrentKafkaListenerContainerFactory<String, RequestBody> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(cf);
        return factory;
    }

}
