package com.cloudera.cyber.nifi;

import org.apache.nifi.components.ValidationContext;
import org.apache.nifi.components.ValidationResult;
import org.apache.nifi.components.Validator;

public class TopicExistsValidator implements Validator {
    @Override
    public ValidationResult validate(String subject, String input, ValidationContext context) {
        return new ValidationResult.Builder().valid(true).build();
    }
}
