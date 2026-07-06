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

package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.geocode.database.IpCompanyEnrichment;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@Slf4j
public class IpCompanyMap extends RichMapFunction<Message, Message> {
    private final String companyDatabasePath;
    private final List<String> ipFieldNames;
    private transient IpCompanyEnrichment companyEnrichment;

    @Override
    public Message map(Message message) {
        Map<String, String> messageFields = message.getExtensions();
        Message newMessage = message;
        List<DataQualityMessage> qualityMessages = new ArrayList<>();
        if (messageFields != null && !ipFieldNames.isEmpty()) {
            Map<String, String> companyExtensions = new HashMap<>();
            for (String ipFieldName : ipFieldNames) {
                Object ipFieldValue = messageFields.get(ipFieldName);
                companyEnrichment.lookup(ipFieldName, ipFieldValue, companyExtensions, qualityMessages);
            }
            newMessage = MessageUtils.enrich(message, companyExtensions, qualityMessages);
        }
        return newMessage;
    }

    @Override
    public void open(Configuration config) {
        try {
            this.companyEnrichment = new IpCompanyEnrichment(companyDatabasePath);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Could not read company database %s", companyDatabasePath), e);
        }
    }

    @Override
    public void close() throws Exception {
        if (this.companyEnrichment != null) {
            companyEnrichment.close();
        }
    }
}
