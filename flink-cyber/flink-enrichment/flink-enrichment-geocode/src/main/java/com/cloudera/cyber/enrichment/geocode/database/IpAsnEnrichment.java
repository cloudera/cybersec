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

package com.cloudera.cyber.enrichment.geocode.database;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import com.cloudera.cyber.enrichment.geocode.database.types.asn.AsnDatabase;
import com.maxmind.db.DatabaseRecord;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.BiFunction;

public class IpAsnEnrichment extends IpEnrichment {
    static final String ASN_FAILED_MESSAGE = "ASN lookup failed '%s'";
    public static final String ASN_FEATURE = "asn";
    public static final String ASN_NUMBER_PREFIX = "number";
    public static final String ASN_ORG_PREFIX = "org";
    public static final String ASN_MASK_PREFIX = "mask";
    private final AsnDatabase database;

    public IpAsnEnrichment(String path) {
        this.database = new AsnDatabase(path);
    }

    @SuppressWarnings("unchecked")
    public void lookup(Enrichment enrichment, Object ipFieldValue, Map<String, String> extensions, List<DataQualityMessage> qualityMessages) {
        InetAddress ipAddress = convertToIpAddress(enrichment, ipFieldValue, qualityMessages);
        if (ipAddress != null) {
            try {
                //noinspection rawtypes
                DatabaseRecord<Map> response = database.lookup(ipAddress);
                if (response != null) {
                    Map<String, Object> data = response.data();
                    if (data != null) {
                        enrichment.enrich(extensions, ASN_NUMBER_PREFIX, this.database.getAsnNumber(response));
                        enrichment.enrich(extensions, ASN_ORG_PREFIX, this.database.getAutonomousSystemOrganization(response));
                        enrichment.enrich(extensions, ASN_MASK_PREFIX, this.database.getNetworkMask(response));
                    }
                }
            } catch (Exception e) {
                enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.ERROR, String.format(ASN_FAILED_MESSAGE, e.getMessage()));
            }
        }
    }

    public void lookup(String fieldName, Object ipFieldValue, Map<String, String> extensions, List<DataQualityMessage> qualityMessages) {
        lookup(SingleValueEnrichment::new, fieldName, ipFieldValue, extensions, qualityMessages);

    }

    public void lookup(BiFunction<String, String, Enrichment> enrichmentBiFunction, String fieldName, Object ipFieldValue, Map<String, String> extensions, List<DataQualityMessage> qualityMessages) {
        if (ipFieldValue instanceof Collection) {
            Enrichment enrichment = enrichmentBiFunction.apply(fieldName, ASN_FEATURE);
            //noinspection unchecked
            ((Collection<Object>) ipFieldValue).forEach(ip -> lookup(enrichment, ip, extensions, qualityMessages));
        }
        else if (ipFieldValue != null) {
            lookup(enrichmentBiFunction.apply(fieldName, ASN_FEATURE), ipFieldValue, extensions, qualityMessages);
        }
    }

    @Override
    public void close() throws IOException {
        this.database.close();
    }
}
