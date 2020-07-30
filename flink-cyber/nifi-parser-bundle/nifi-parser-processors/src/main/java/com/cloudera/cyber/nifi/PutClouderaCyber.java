package com.cloudera.cyber.nifi;

import org.apache.nifi.annotation.behavior.*;
import org.apache.nifi.annotation.documentation.CapabilityDescription;
import org.apache.nifi.annotation.documentation.Tags;
import org.apache.nifi.components.PropertyDescriptor;
import org.apache.nifi.expression.ExpressionLanguageScope;
import org.apache.nifi.processor.*;
import org.apache.nifi.processor.exception.ProcessException;

import java.util.*;

import static org.apache.nifi.annotation.behavior.InputRequirement.Requirement;

@EventDriven
@SideEffectFree
@SupportsBatching
@InputRequirement(Requirement.INPUT_REQUIRED)
@Tags({"put", "cyber"})
@CapabilityDescription("Sends Data to Cloudera Cyber processing")
@DynamicProperty(name = "The name of a Kafka configuration property.", value = "The value of a given Kafka configuration property.",
        description = "These properties will be added on the Kafka configuration after loading any provided configuration properties."
                + " In the event a dynamic property represents a property that was already set, its value will be ignored and WARN message logged."
                + " For the list of available Kafka properties please refer to: http://kafka.apache.org/documentation.html#configuration. ",
        expressionLanguageScope = ExpressionLanguageScope.VARIABLE_REGISTRY)
@WritesAttribute(attribute = "msg.count", description = "The number of messages that were sent to Kafka for this FlowFile. This attribute is added only to "
        + "FlowFiles that are routed to success. If the <Message Demarcator> Property is not set, this will always be 1, but if the Property is set, it may "
        + "be greater than 1.")
public class PutClouderaCyber extends AbstractProcessor  {


    public static final Relationship REL_SUCCESS = new Relationship.Builder()
            .name("success")
            .description("Any FlowFile that is successfully sent will be routed to success")
            .build();
    public static final Relationship REL_FAILURE = new Relationship.Builder()
            .name("failure")
            .description("Any FlowFile that cannot be sent will be routed to failure")
            .build();
    private static final PropertyDescriptor TOPIC_NAME = new PropertyDescriptor.Builder()
            .name("topic")
            .displayName("Kafka Topic")
            .required(true)
            .addValidator(new TopicExistsValidator())
            .expressionLanguageSupported(ExpressionLanguageScope.FLOWFILE_ATTRIBUTES)
            .build();
    private static final PropertyDescriptor CLOUDERA_CYBER = new PropertyDescriptor.Builder()
            .name("connection")
            .displayName("Cloudera Cyber Connection Service")
            .required(true)
            .expressionLanguageSupported(ExpressionLanguageScope.NONE)
            .identifiesControllerService(ClouderaCyberConnectionService.class)
            .build();


    private List<PropertyDescriptor> properties;
    private Set<Relationship> relationships;

    @Override
    public Set<Relationship> getRelationships() {
        return relationships;
    }

    @Override
    protected List<PropertyDescriptor> getSupportedPropertyDescriptors() {
        return properties;
    }

    @Override
    protected void init(final ProcessorInitializationContext context) {
        final List<PropertyDescriptor> props = new ArrayList<>();
        props.add(CLOUDERA_CYBER);
        props.add(TOPIC_NAME);
        this.properties = Collections.unmodifiableList(props);

        final Set<Relationship> rels = new HashSet<>();
        rels.add(REL_SUCCESS);
        rels.add(REL_FAILURE);
        this.relationships = Collections.unmodifiableSet(rels);
    }
    @Override
    public void onTrigger(ProcessContext context, ProcessSession session) throws ProcessException {

    }
}
