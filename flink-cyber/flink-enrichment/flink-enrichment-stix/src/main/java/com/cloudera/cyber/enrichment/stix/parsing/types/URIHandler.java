package com.cloudera.cyber.enrichment.stix.parsing.types;

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.Parser;
import com.google.common.collect.ImmutableList;
import org.mitre.cybox.common_2.AnyURIObjectPropertyType;
import org.mitre.cybox.objects.URIObjectType;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class URIHandler extends AbstractObjectTypeHandler<URIObjectType> {

    public URIHandler() {
        super(URIObjectType.class);
    }

    @Override
    public Stream<ThreatIntelligence.ThreatIntelligenceBuilder> extract(URIObjectType type, Map<String, Object> config) {
        AnyURIObjectPropertyType value = type.getValue();

        return StreamSupport.stream(Parser.split(value).spliterator(), false)
                .map(mapToThreatIntelligence("URIObject:URIObjectType"));
    }

    @Override
    public List<String> getPossibleTypes() {
        return ImmutableList.of(getType());
    }
}
