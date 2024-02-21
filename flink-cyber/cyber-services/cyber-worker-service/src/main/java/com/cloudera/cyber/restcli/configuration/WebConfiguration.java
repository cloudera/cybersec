package com.cloudera.cyber.restcli.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.metron.common.utils.JSONUtils;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;

@Configuration
public class WebConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        return JSONUtils.INSTANCE.getMapper();
    }

    @Bean
    public RestTemplate restTemplate() {

        return new RestTemplateBuilder()
                .setConnectTimeout(Duration.ofSeconds(120))
                .setReadTimeout(Duration.ofSeconds(120))
                .build();
    }
}
