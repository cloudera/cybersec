package com.cloudera.cyber.nifi;

import com.cloudera.cyber.Message;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.schemaregistry.services.SchemaRegistry;

import java.util.List;

/**
 * Connection service for the very opinionated way we setup and connect to Cyber Clusters
 * <p>
 * This will also build the Kafka Producers and predetermine the schema registry requirements, kerberos etc.
 */
public class CDPDCCyberConnectionService extends AbstractCyberConnectionService {
    @Override
    public KafkaProducer<String, Message> kafkaProducer() {
        return null;
    }

    @Override
    public SchemaRegistry schemaRegistry() {
        return null;
    }

}
