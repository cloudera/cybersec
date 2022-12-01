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

import lombok.*;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.apache.commons.text.StringSubstitutor;
import com.fasterxml.jackson.annotation.JsonSubTypes;

import java.io.Serializable;

@Data
@JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        property = "type")
@JsonSubTypes({
        @JsonSubTypes.Type(value = BasicAuthorizationConfig.class, name = "basic"),
        @JsonSubTypes.Type(value = BearerTokenAuthorizationConfig.class, name = "token")
})
public abstract class EndpointAuthorizationConfig implements Serializable {

    public abstract String generateAuthString(StringSubstitutor stringSubstitutor);

}