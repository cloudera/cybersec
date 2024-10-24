package com.cloudera.cyber.generator;

import java.io.IOException;
import java.util.List;
import lombok.Data;

@Data
public class GeneratorConfig {
    String baseDirectory;
    List<GenerationSource> generationSources;

    public void open() throws IOException {
        for (GenerationSource source : getGenerationSources()) {
            source.readAvroSchema(baseDirectory);
        }
    }
}
