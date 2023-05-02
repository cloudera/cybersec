package com.cloudera.cyber.indexing;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.util.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingColumnDto {

    @JsonProperty("name")
    private String name;

    @JsonProperty("kafka_name")
    private String kafkaName;

    @JsonProperty("path")
    private String path;

    @JsonProperty("transformation")
    private String transformation;

    @JsonProperty("is_map")
    private Boolean isMap;

    public String getKafkaName() {
        final String properName = kafkaName == null ? name : kafkaName;
        if (getIsMap()) {
            return String.format("['%s']", properName);
        } else {
            if (getPath().equals("..")) {
                return String.format("%s", properName);
            }
            return String.format(".%s", properName);
        }
    }

    public String getRawKafkaName(){
        return kafkaName;
    }

    public String getPath() {
        return StringUtils.hasText(path)
                ? (path.equals(".") ? "" : path)
                : "extensions";
    }

    public Boolean getIsMap() {
        return isMap == null ? path == null : isMap;
    }
}
