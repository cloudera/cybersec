package com.cloudera.cyber.restcli.configuration;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.metron.common.utils.JSONUtils;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class WebConfiguration {
    @Bean
    public ObjectMapper objectMapper() {
        return JSONUtils.INSTANCE.getMapper();
    }

}
