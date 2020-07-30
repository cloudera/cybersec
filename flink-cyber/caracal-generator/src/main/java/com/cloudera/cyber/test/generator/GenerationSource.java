package com.cloudera.cyber.test.generator;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.io.Serializable;

@Data
@AllArgsConstructor
public class GenerationSource implements Serializable {
    private String file;
    private String topic;
}
