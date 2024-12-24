package com.cloudera.cyber.indexing;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingDto {

    @JsonProperty("table_name")
    private String tableName;

    @JsonProperty("ignore_fields")
    private List<String> ignoreFields;

    @JsonProperty("column_mapping")
    private List<MappingColumnDto> columnMapping;

    public Set<String> getIgnoreFields() {
        return ignoreFields == null ? Collections.singleton("") :
              Stream.concat(ignoreFields.stream(), columnMapping.stream()
                          .filter(dto -> "extensions".equals(dto.getPath()) && dto.getIsMap())
                          .map(dto -> Optional.ofNullable(dto.getRawKafkaName()).orElse(dto.getName())))
                    .filter(Objects::nonNull)
                    .map(String::toLowerCase)
                    .collect(Collectors.toSet());
    }
}
