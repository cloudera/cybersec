package com.cloudera.cyber.nifi;

import com.cloudera.cyber.Message;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.controller.ControllerService;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.schemaregistry.services.SchemaRegistry;

public interface ClouderaCyberConnectionService extends ControllerService {

    KafkaProducer<String, Message> kafkaProducer();

    SchemaRegistry schemaRegistry();


}
