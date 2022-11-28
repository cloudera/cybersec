/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

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
    public Stream<ThreatIntelligence.ThreatIntelligenceBuilder> extract(DomainName type, Map<String, Object> config) {
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
