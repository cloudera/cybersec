package com.cloudera.cyber.indexing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

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

    @JsonIgnore
    public List<String> getKafkaNameList() {
        final String properName = kafkaName == null ? name : kafkaName;
        if (getIsMap()) {
            return Collections.singletonList(String.format("['%s']", properName));
        }

        String[] kafkaNamesSplit = properName.split(",");

        return Arrays.stream(kafkaNamesSplit)
              .map(singleKafkaName -> {
                  if (getPath().equals("..")) {
                      return String.format("%s", singleKafkaName);
                  }
                  return String.format(".%s", singleKafkaName);
              })
              .collect(Collectors.toList());
    }

    @JsonIgnore
    public String getRawKafkaName() {
        return kafkaName;
    }

    @JsonProperty("path")
    public String getRawPath() {
        return this.path;
    }

    @JsonIgnore
    public String getPath() {
        if (StringUtils.isEmpty(path)) {
            return "extensions";
        } else if (path.equals(".")) {
            return "";
        }
        return path;
    }

    public boolean getIsMap() {
        return isMap == null ? path == null : isMap;
    }
}
