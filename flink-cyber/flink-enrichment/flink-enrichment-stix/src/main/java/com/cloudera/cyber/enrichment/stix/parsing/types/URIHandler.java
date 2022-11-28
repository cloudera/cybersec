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
