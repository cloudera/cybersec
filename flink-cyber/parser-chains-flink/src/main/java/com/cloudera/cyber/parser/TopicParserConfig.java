package com.cloudera.cyber.parser;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TopicParserConfig implements Serializable {
    private String chainKey;
    private String source;
    private String broker;
}
