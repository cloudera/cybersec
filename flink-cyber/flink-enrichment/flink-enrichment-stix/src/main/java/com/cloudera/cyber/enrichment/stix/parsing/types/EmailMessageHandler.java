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
import org.mitre.cybox.objects.Address;
import org.mitre.cybox.objects.EmailMessage;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

public class EmailMessageHandler extends AbstractObjectTypeHandler<EmailMessage> {
    public EmailMessageHandler() {
        super(EmailMessage.class);
    }

    @Override
    public Stream<ThreatIntelligence.ThreatIntelligenceBuilder> extract(EmailMessage type, Map<String, Object> config) {
        List<Address> tos = type.getHeader().getTo().getRecipients();
        List<Address> cc = type.getHeader().getCC().getRecipients();
        List<Address> bcc = type.getHeader().getBCC().getRecipients();
        Address from = type.getHeader().getFrom();

        return Stream.of(tos, cc, bcc, Arrays.asList(from)).flatMap(f -> f.stream()).flatMap(a ->
                        StreamSupport.stream(Parser.split(a.getAddressValue()).spliterator(), false)
            .map(mapToThreatIntelligence("Address:e-mail"))
        );
    }


    @Override
    public List<String> getPossibleTypes() {
        return ImmutableList.of("EmailMessageObj:EmailMessageObjectType");
    }
}
