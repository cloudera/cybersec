package com.cloudera.cyber.rest.scoring;

import com.cloudera.cyber.rules.DynamicRuleCommandResult;
import com.cloudera.cyber.rules.DynamicRuleCommandType;
import com.cloudera.cyber.scoring.ScoringRule;
import com.cloudera.cyber.scoring.ScoringRuleCommand;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.requestreply.ReplyingKafkaTemplate;
import org.springframework.kafka.requestreply.RequestReplyFuture;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutionException;

@Controller
public class ScoringRules {
    @Autowired
    ReplyingKafkaTemplate<UUID, ScoringRuleCommand, DynamicRuleCommandResult<ScoringRule>> kafkaTemplate;

    @Value("${scoring.command.output}")
    private String requestReplyTopic;

    @Value("${scoring.command.input}")
    private String commandTopic;

    @GetMapping("/scoring/{id}")
    public DynamicRuleCommandResult<ScoringRule> get(@PathVariable UUID id) throws ExecutionException, InterruptedException {
        return sendCommand((ScoringRuleCommand.ScoringRuleCommandBuilder<?, ?>) ScoringRuleCommand.builder().ruleId(id).type(DynamicRuleCommandType.GET));
    }

    @DeleteMapping("/scoring/{id}")
    public void delete(@PathVariable UUID id) throws ExecutionException, InterruptedException {
        sendCommand((ScoringRuleCommand.ScoringRuleCommandBuilder<?, ?>) ScoringRuleCommand.builder().ruleId(id).type(DynamicRuleCommandType.DELETE));
    }

    @PutMapping("/scoring/{id}")
    public DynamicRuleCommandResult<ScoringRule> put(@RequestBody ScoringRule rule) throws ExecutionException, InterruptedException {
        return sendCommand((ScoringRuleCommand.ScoringRuleCommandBuilder<?, ?>) ScoringRuleCommand.builder().rule(rule).ruleId(rule.getId()).type(DynamicRuleCommandType.UPSERT));
    }

    @PutMapping("/scoring/{id}/enable")
    public DynamicRuleCommandResult<ScoringRule> enable(@PathVariable UUID id) throws ExecutionException, InterruptedException {
        return sendCommand((ScoringRuleCommand.ScoringRuleCommandBuilder<?, ?>) ScoringRuleCommand.builder().ruleId(id).type(DynamicRuleCommandType.ENABLE));
    }

    @PutMapping("/scoring/{id}/disable")
    public DynamicRuleCommandResult<ScoringRule> disable(@PathVariable UUID id) throws ExecutionException, InterruptedException {
        return sendCommand((ScoringRuleCommand.ScoringRuleCommandBuilder<?, ?>) ScoringRuleCommand.builder().ruleId(id).type(DynamicRuleCommandType.DISABLE));
    }

    @GetMapping("/scoring")
    public List<ScoringRule> list() throws ExecutionException, InterruptedException {
        sendCommand((ScoringRuleCommand.ScoringRuleCommandBuilder<?, ?>) ScoringRuleCommand.builder().type(DynamicRuleCommandType.LIST));
        return Collections.emptyList();
    }

    private DynamicRuleCommandResult<ScoringRule> sendCommand(ScoringRuleCommand.ScoringRuleCommandBuilder type) throws ExecutionException, InterruptedException {
        UUID cmdId = UUID.randomUUID();
        ScoringRuleCommand command = (ScoringRuleCommand) type.id(cmdId).ts(Instant.now()).build();

        ProducerRecord<UUID, ScoringRuleCommand> record = new ProducerRecord<UUID, ScoringRuleCommand>(commandTopic, cmdId, command);
        record.headers().add(new RecordHeader(KafkaHeaders.REPLY_TOPIC, requestReplyTopic.getBytes()));

        RequestReplyFuture<UUID, ScoringRuleCommand, DynamicRuleCommandResult<ScoringRule>> sendAndReceive = kafkaTemplate.sendAndReceive(record);
        ConsumerRecord<UUID, DynamicRuleCommandResult<ScoringRule>> consumerRecord = sendAndReceive.get();
        return consumerRecord.value();
    }
}
