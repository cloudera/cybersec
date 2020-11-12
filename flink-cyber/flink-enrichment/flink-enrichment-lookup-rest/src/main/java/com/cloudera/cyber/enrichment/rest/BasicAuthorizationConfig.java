package com.cloudera.cyber.enrichment.rest;

import lombok.*;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.text.StringSubstitutor;
import org.apache.flink.util.Preconditions;
import org.apache.http.client.config.AuthSchemes;

import java.nio.charset.StandardCharsets;

/**
 * Credentials required for HTTP basic authorization.
 */
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@NoArgsConstructor(force = true, access = AccessLevel.PUBLIC)
@Builder
@Data
public class BasicAuthorizationConfig extends EndpointAuthorizationConfig {
    /** template resulting in the user name of the rest service basic auth credential. */
    private String userNameTemplate;

    /** template resulting in the password of the rest service basic auth credential. */
    private String passwordTemplate;


    @Override
    public String generateAuthString(StringSubstitutor stringSubstitutor) {
        Preconditions.checkNotNull(userNameTemplate);
        Preconditions.checkNotNull(passwordTemplate);
        byte[] credentials = Base64.encodeBase64(String.join(":", stringSubstitutor.replace(userNameTemplate),
                stringSubstitutor.replace(passwordTemplate)).getBytes(StandardCharsets.ISO_8859_1));
        return String.join(" ", AuthSchemes.BASIC, new String(credentials));
    }
}
