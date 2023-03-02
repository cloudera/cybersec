package com.cloudera.cyber.indexing.hive.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MappingDto {

    @JsonProperty("hive_table")
    private String hiveTable;

    @JsonProperty("ignore_fields")
    private List<String> ignoreFields;

    @JsonProperty("column_mapping")
    private List<MappingColumnDto> columnMapping;

}
