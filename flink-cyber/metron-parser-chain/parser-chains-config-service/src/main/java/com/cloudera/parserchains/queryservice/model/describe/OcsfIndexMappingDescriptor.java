package com.cloudera.parserchains.queryservice.model.describe;

import com.cloudera.cyber.indexing.MappingDto;
import java.util.Map;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class OcsfIndexMappingDescriptor extends IndexMappingDescriptor {

    private Map<String, MappingDto> mappings;

}
