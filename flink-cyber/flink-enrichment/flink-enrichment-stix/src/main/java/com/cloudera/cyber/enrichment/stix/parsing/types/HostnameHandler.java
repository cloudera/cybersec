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
