package com.cloudera.parserchains.queryservice.model.describe;

import com.cloudera.cyber.indexing.MappingDto;
import lombok.Getter;
import lombok.Setter;

import java.util.Map;

@Getter
@Setter
public class OcsfIndexMappingDescriptor extends IndexMappingDescriptor {

    private Map<String, MappingDto> mappings;

}
