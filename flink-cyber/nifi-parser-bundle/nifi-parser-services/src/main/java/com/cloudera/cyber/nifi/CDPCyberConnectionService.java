package com.cloudera.cyber.nifi;

import com.cloudera.cyber.Message;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.schemaregistry.services.SchemaRegistry;

public class CDPCyberConnectionService extends AbstractCyberConnectionService {
    @Override
    public KafkaProducer<String, Message> kafkaProducer() {
        return null;
    }

    @Override
    public SchemaRegistry schemaRegistry() {
        return null;
    }
}
