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
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.util.Preconditions;

@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@Builder
@Data
public class BearerTokenAuthorizationConfig extends EndpointAuthorizationConfig {
    private String bearerTokenTemplate;

    @Override
    public String generateAuthString(StringSubstitutor stringSubstitutor) {
        Preconditions.checkNotNull(bearerTokenTemplate);
        return "Bearer ".concat(stringSubstitutor.replace(bearerTokenTemplate));
    }
}
