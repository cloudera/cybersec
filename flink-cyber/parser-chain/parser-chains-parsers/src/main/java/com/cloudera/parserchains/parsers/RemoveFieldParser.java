package com.cloudera.parserchains.parsers;

import com.cloudera.parserchains.core.FieldName;
import com.cloudera.parserchains.core.Message;
import com.cloudera.parserchains.core.Parser;
import com.cloudera.parserchains.core.catalog.Configurable;
import com.cloudera.parserchains.core.catalog.MessageParser;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * A parser which can remove fields from a message.
 */
@MessageParser(
    name="Remove Field(s)",
    description="Removes unwanted message field(s).")
public class RemoveFieldParser implements Parser {
    private List<FieldName> fieldsToRemove;

    public RemoveFieldParser() {
        fieldsToRemove = new ArrayList<>();
    }

    public RemoveFieldParser removeField(FieldName fieldName) {
        fieldsToRemove.add(fieldName);
        return this;
    }

    @Override
    public Message parse(Message message) {
        return Message.builder()
                .withFields(message)
                .removeFields(fieldsToRemove)
                .build();
    }

    List<FieldName> getFieldsToRemove() {
        return fieldsToRemove;
    }

    @Configurable(
            key="fieldToRemove",
            label="Field to Remove",
            description="The name of a field to remove.",
            required=true)
    public void removeField(String fieldName) {
        if(StringUtils.isNotBlank(fieldName)) {
            removeField(FieldName.of(fieldName));
        }
    }
}
