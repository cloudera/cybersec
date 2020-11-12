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
