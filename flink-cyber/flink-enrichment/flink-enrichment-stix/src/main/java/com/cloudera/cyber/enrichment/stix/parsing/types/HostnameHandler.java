package com.cloudera.cyber.enrichment.stix.parsing.types;

import com.cloudera.cyber.ThreatIntelligence;
import com.google.common.collect.ImmutableList;
import org.mitre.cybox.objects.Hostname;

import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

public class HostnameHandler extends AbstractObjectTypeHandler<Hostname> {

    public HostnameHandler() {
        super(Hostname.class);
    }

    @Override
    public Stream<ThreatIntelligence.ThreatIntelligenceBuilder> extract(Hostname type, Map<String, Object> config) {
        return null;
    }

    @Override
    public List<String> getPossibleTypes() {
        return ImmutableList.of(getType());
    }

}
