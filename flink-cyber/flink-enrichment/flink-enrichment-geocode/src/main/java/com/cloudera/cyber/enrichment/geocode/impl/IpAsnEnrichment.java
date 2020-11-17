package com.cloudera.cyber.enrichment.geocode.impl;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.SingleValueEnrichment;
import com.maxmind.geoip2.DatabaseProvider;
import com.maxmind.geoip2.model.AsnResponse;

import java.net.InetAddress;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public class IpAsnEnrichment extends MaxMindBase {
    private static final String ASN_FAILED_MESSAGE = "ASN lookup failed %s";
    private static final String ASN_FEATURE = "asn";

    public IpAsnEnrichment(DatabaseProvider database) {
        super(database);
    }

    public void lookup(Enrichment enrichment, String ipFieldName, Object ipFieldValue, Map<String, String> extensions, List<DataQualityMessage> qualityMessages) {
        InetAddress ipAddress = convertToIpAddress(enrichment, ipFieldValue, qualityMessages);
        if (ipAddress != null) {
            try {
                Optional<AsnResponse> response = database.tryAsn(ipAddress);
                response.ifPresent(r -> {
                    enrichment.enrich(extensions, "asn", r.getAutonomousSystemNumber());
                    enrichment.enrich(extensions, "asn.org", r.getAutonomousSystemOrganization());
                    enrichment.enrich(extensions, "asn.mask", r.getNetwork().toString());
                });
            } catch (Exception e) {
                enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.ERROR, String.format(ASN_FAILED_MESSAGE, e.getMessage()));
            }
        }
    }

    public void lookup(String fieldName, Object ipFieldValue, Map<String, String> geoEnrichments, List<DataQualityMessage> qualityMessages) {
        lookup(new SingleValueEnrichment(fieldName, ASN_FEATURE), fieldName, ipFieldValue, geoEnrichments, qualityMessages);
    }
}
