package com.cloudera.cyber.indexing;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingColumnDto {

    public static final String EXTENSIONS_PATH = "extensions";
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
        final String properName = getProperName();
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
            return EXTENSIONS_PATH;
        } else if (path.equals(".")) {
            return "";
        }
        return path;
    }

    private String getProperName() {
        return kafkaName == null ? name : kafkaName;
    }

    public boolean getIsMap() {
        return isMap == null ? path == null : isMap;
    }

    public MappingColumnDto deriveUnenriched(List<String> enrichedFieldPrefixes, Set<String> sourceEnrichedPrefixes) {
        MappingColumnDto unenrichedMapping = null;
        if (EXTENSIONS_PATH.equals(getPath())) {
            String properName = getProperName();
            if ((enrichedFieldPrefixes == null || enrichedFieldPrefixes.stream().noneMatch(properName::startsWith)) &&
                    (sourceEnrichedPrefixes == null || sourceEnrichedPrefixes.stream().noneMatch(properName::startsWith))) {
                unenrichedMapping = new MappingColumnDto(name, kafkaName, path, transformation, isMap);
            }
        } else {
            // enrichment fields will only be in the extensions
            unenrichedMapping = new MappingColumnDto(name, kafkaName, path, transformation, isMap);
        }
        return unenrichedMapping;
    }
}
