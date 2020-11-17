package com.cloudera.cyber.enrichment.stix.parsing.types;

import com.cloudera.cyber.ThreatIntelligence;
import org.mitre.cybox.common_2.ObjectPropertiesType;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public interface ObjectTypeHandler<T extends ObjectPropertiesType> {
    Stream<ThreatIntelligence.ThreatIntelligenceBuilder> extract(T type, Map<String, Object> config);

    Class<T> getTypeClass();

    List<String> getPossibleTypes();
}
