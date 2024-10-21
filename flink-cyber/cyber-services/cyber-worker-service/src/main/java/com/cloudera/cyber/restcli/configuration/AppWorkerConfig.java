package com.cloudera.cyber.restcli.configuration;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConfigurationProperties(prefix = "cluster")
@Getter
@Setter
public class AppWorkerConfig {
    private String name;
    private String id;
    private String status;
    private String version;
    private String pipelineDir;
}
