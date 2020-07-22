package com.cloudera.cyber.rest.scoring;

import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import com.cloudera.cyber.scoring.ScoringRule;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.ConcurrentKafkaListenerContainerFactory;
import org.springframework.kafka.config.KafkaListenerContainerFactory;
import org.springframework.kafka.core.*;
import org.springframework.kafka.listener.ConcurrentMessageListenerContainer;
import org.springframework.kafka.listener.ContainerProperties;
import org.springframework.kafka.listener.KafkaMessageListenerContainer;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;

import java.util.Map;
import java.util.UUID;

@Configuration
public class KafkaConfigScoring {

    @Autowired
    Map<String, Object> producerConfigs;

    @Autowired
    Map<String, Object> consumerConfigs;

    @Value("${scoring.command.output}")
    private String requestReplyTopic;

    @Bean
    public ProducerFactory<UUID, ScoringRuleCommand> producerFactory() {
        return new DefaultKafkaProducerFactory<>(producerConfigs);
    }

    @Bean
    public KafkaTemplate<UUID, ScoringRuleCommand> kafkaTemplate() {
        return new KafkaTemplate<>(producerFactory());
    }

    @Bean
    public ReplyingKafkaTemplate<UUID, ScoringRuleCommand, DynamicRuleCommandResult<ScoringRule>> replyKafkaTemplate(
            ProducerFactory<UUID, ScoringRuleCommand> pf,
            KafkaMessageListenerContainer<UUID, DynamicRuleCommandResult<ScoringRule>> container) {
        return new ReplyingKafkaTemplate<>(pf, container);
    }

    @Bean
    public KafkaMessageListenerContainer<UUID, DynamicRuleCommandResult<ScoringRule>> replyContainer(ConsumerFactory<UUID, DynamicRuleCommandResult<ScoringRule>> cf) {
        ContainerProperties containerProperties = new ContainerProperties(requestReplyTopic);
        return new KafkaMessageListenerContainer<>(cf, containerProperties);
    }

    @Bean
    public ConsumerFactory<UUID, DynamicRuleCommandResult<ScoringRule>> consumerFactory() {
        return new DefaultKafkaConsumerFactory<>(consumerConfigs);
    }

    @Bean
    public KafkaListenerContainerFactory<ConcurrentMessageListenerContainer<UUID, DynamicRuleCommandResult<ScoringRule>>> kafkaListenerContainerFactory() {
        ConcurrentKafkaListenerContainerFactory<UUID, DynamicRuleCommandResult<ScoringRule>> factory = new ConcurrentKafkaListenerContainerFactory<>();
        factory.setConsumerFactory(consumerFactory());
        factory.setReplyTemplate(kafkaTemplate());
        return factory;
    }
}
