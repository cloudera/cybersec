package com.cloudera.parserchains.queryservice.model.describe;

import com.cloudera.cyber.indexing.MappingDto;
import com.cloudera.cyber.indexing.TableColumnDto;
import java.util.List;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OcsfIndexMappingDescriptor {

    private String tableFilePath;
    private String mappingFilePath;
    private Map<String, MappingDto> mappings;
    private Map<String, List<TableColumnDto>> tableConfig;

}
