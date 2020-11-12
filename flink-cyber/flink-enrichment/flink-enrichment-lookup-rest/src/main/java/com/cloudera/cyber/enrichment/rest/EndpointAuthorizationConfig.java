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