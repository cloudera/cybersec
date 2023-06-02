package com.cloudera.cyber.generator;

import lombok.Data;

import java.io.IOException;
import java.util.List;

@Data
public class GeneratorConfig {
    String baseDirectory;
    List<GenerationSource> generationSources;

    public void open() throws IOException {
        for(GenerationSource source : getGenerationSources()) {
            source.readAvroSchema(baseDirectory);
        }
    }
}
