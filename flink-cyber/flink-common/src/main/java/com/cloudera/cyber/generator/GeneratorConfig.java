package com.cloudera.cyber.generator;

import lombok.Data;
import org.apache.flink.core.fs.Path;

import java.io.IOException;
import java.util.List;

@Data
public class GeneratorConfig {
    String baseDirectory;
    List<GenerationSource> generationSources;

    public void open(Path configPath) throws IOException {
        for(GenerationSource source : getGenerationSources()) {
            String configDirectory = (configPath != null) ? new Path(configPath.getParent(), baseDirectory).toString() : baseDirectory;
            source.readAvroSchema(configDirectory);
        }
    }
}
