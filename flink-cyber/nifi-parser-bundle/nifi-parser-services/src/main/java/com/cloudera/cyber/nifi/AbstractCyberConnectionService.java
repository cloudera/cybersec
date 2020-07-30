package com.cloudera.cyber.nifi;

import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.controller.AbstractControllerService;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.schemaregistry.services.SchemaRegistry;
import org.apache.nifi.ssl.SSLContextService;

import java.util.List;

public abstract class AbstractCyberConnectionService extends AbstractControllerService implements ClouderaCyberConnectionService {

    static final PropertyDescriptor SR = new PropertyDescriptor.Builder()
            .name("Schema Registry")
            .displayName("Schema Registry")
            .description("Service for Schema Registry Access")
            .expressionLanguageSupported(ExpressionLanguageScope.VARIABLE_REGISTRY)
            .identifiesControllerService(SchemaRegistry.class)
            .required(true)
            .build();

    static final PropertyDescriptor SSL_CONTEXT_SERVICE = new PropertyDescriptor.Builder()
            .name("ssl-context-service")
            .displayName("SSL Context Service")
            .description("Specifies the SSL Context Service to use for communicating with Schema Registry.")
            .required(false)
            .identifiesControllerService(SSLContextService.class)
            .build();

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return super.getSupportedPropertyDescriptors();
    }

    @Override
    protected PropertyDescriptor getSupportedDynamicPropertyDescriptor(String propertyDescriptorName) {
        return super.getSupportedDynamicPropertyDescriptor(propertyDescriptorName);
    }

}
