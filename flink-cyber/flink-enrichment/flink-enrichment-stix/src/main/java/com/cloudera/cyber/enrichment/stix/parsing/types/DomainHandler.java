package com.cloudera.cyber.enrichment.stix.parsing.types;

import com.cloudera.cyber.ThreatIntelligence;
import com.cloudera.cyber.enrichment.stix.parsing.Parser;
import org.mitre.cybox.common_2.StringObjectPropertyType;
import org.mitre.cybox.objects.DomainName;
import org.mitre.cybox.objects.DomainNameTypeEnum;

import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class DomainHandler extends AbstractObjectTypeHandler<DomainName> {
    EnumSet<DomainNameTypeEnum> SUPPORTED_TYPES = EnumSet.of(DomainNameTypeEnum.FQDN);

    public DomainHandler() {
        super(DomainName.class);
    }

    @Override
    public Stream<ThreatIntelligence.Builder> extract(DomainName type, Map<String, Object> config) {
        final DomainNameTypeEnum domainType = type.getType();
        List<ThreatIntelligence> output = new ArrayList<>();
        if (domainType == null || SUPPORTED_TYPES.contains(domainType)) {
            StringObjectPropertyType value = type.getValue();
            return StreamSupport.stream(Parser.split(value).spliterator(), false)
                    .map(mapToThreatIntelligence("DomainNameObj" + ":" + DomainNameTypeEnum.FQDN));
        }
        return Stream.empty();
    }

    @Override
    public List<String> getPossibleTypes() {
        String typeStr = getType();
        List<String> ret = new ArrayList<>();
        for (DomainNameTypeEnum e : SUPPORTED_TYPES) {
            ret.add(typeStr + ":" + e);
        }
        return ret;
    }
}
