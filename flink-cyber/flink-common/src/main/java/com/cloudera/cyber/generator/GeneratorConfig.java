package com.cloudera.cyber.generator;

import lombok.Data;

import java.util.List;

@Data
public class GeneratorConfig {
    String templateBaseDirectory;
    List<GenerationSource> generationSources;
}
