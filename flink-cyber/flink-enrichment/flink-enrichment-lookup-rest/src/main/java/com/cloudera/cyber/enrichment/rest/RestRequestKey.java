/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.rest;

import com.google.common.base.Joiner;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.text.StringSubstitutor;
import org.apache.commons.text.lookup.StringLookup;
import org.apache.commons.text.lookup.StringLookupFactory;

@Data
@EqualsAndHashCode
public class RestRequestKey {
    public static final String UNDEFINED_VARIABLE_MESSAGE = "Variable(s) '%s' required by rest %s are undefined";
    public static final String URL_SOURCE = "url";
    public static final String ENTITY_SOURCE = "entity";
    public static final String INVALID_URI_ERROR = "Rest URI '%s' is invalid.";
    private URI restUri;
    private String entity;
    private final List<String> errors = new ArrayList<>();

    private static class TrackingSubstitutor implements StringLookup {
        private Set<String> undefinedVariables = new HashSet<>();
        private final StringLookup baseLookup;
        private final StringSubstitutor substitutor;

        public TrackingSubstitutor(Map<String, String> variables) {
            baseLookup = StringLookupFactory.INSTANCE.mapStringLookup(variables);
            substitutor = new StringSubstitutor().setVariableResolver(this);
        }

        @Override
        public String lookup(String s) {
            String value = baseLookup.lookup(s);
            if (StringUtils.isBlank(value)) {
                value = null;
                undefinedVariables.add(s);
            }
            return value;
        }

        public String replace(String template) {
            undefinedVariables.clear();
            String replacedString = substitutor.replace(template);
            // filter out any variables where the template provided a default value
            undefinedVariables = undefinedVariables.stream()
                                                   .filter((varName) -> replacedString.contains("${".concat(varName)))
                                                   .collect(Collectors.toSet());

            return replacedString;
        }

        public String getUndefinedVariables() {
            if (!undefinedVariables.isEmpty()) {
                return Joiner.on(",").join(undefinedVariables);
            } else {
                return null;
            }
        }
    }

    public RestRequestKey(Map<String, String> variables, String uriTemplate) {
        TrackingSubstitutor stringSubstitutor = new TrackingSubstitutor(variables);
        validateRestUriTemplate(uriTemplate, stringSubstitutor);
    }

    public RestRequestKey(Map<String, String> variables, String uriTemplate, String entityTemplate) {
        TrackingSubstitutor stringSubstitutor = new TrackingSubstitutor(variables);
        validateRestUriTemplate(uriTemplate, stringSubstitutor);
        validateEntityTemplate(entityTemplate, stringSubstitutor);
    }

    private void validateRestUriTemplate(String uriTemplate, TrackingSubstitutor stringSubstitutor) {
        String uriString = stringSubstitutor.replace(uriTemplate);
        if (!hasUndefinedVariables(stringSubstitutor, URL_SOURCE)) {
            try {
                this.restUri = URI.create(uriString);
            } catch (Exception e) {
                this.restUri = null;
                errors.add(String.format(INVALID_URI_ERROR, uriString));
            }
        }
    }

    private void validateEntityTemplate(String entityTemplate, TrackingSubstitutor stringSubstitutor) {
        entity = stringSubstitutor.replace(entityTemplate);
        hasUndefinedVariables(stringSubstitutor, ENTITY_SOURCE);
    }

    private boolean hasUndefinedVariables(TrackingSubstitutor stringSubstitutor, String sourceStringName) {
        String undefinedVariables = stringSubstitutor.getUndefinedVariables();
        if (undefinedVariables != null) {
            errors.add(String.format(UNDEFINED_VARIABLE_MESSAGE, undefinedVariables, sourceStringName));
        }
        return (undefinedVariables != null);
    }
}
