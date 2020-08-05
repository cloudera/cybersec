package com.cloudera.cyber.parser;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class MessageToParse {
    private String originalSource;
    private String topic;
}
