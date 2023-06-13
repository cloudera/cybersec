package com.cloudera.cyber.enrichment.cidr.impl;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.Enrichment;
import com.cloudera.cyber.enrichment.MetronGeoEnrichment;
import inet.ipaddr.IPAddress;
import inet.ipaddr.IPAddressString;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@AllArgsConstructor
@Slf4j
public class IpRegionCidrEnrichment {
    private Map<IPAddressString, String> regionCidrMap;
    public final static String FEATURE_NAME = "region";


    public void lookup(String ipFieldName, String ipFieldValue, Map<String, String> extensions, List<DataQualityMessage> qualityMessages) {
        IPAddressString ipAddressString = new IPAddressString(ipFieldValue);
        Enrichment enrichment = new MetronGeoEnrichment(ipFieldName, null);
        if (ipAddressString.getAddress() == null) {
            MessageUtils.addQualityMessage(qualityMessages, DataQualityMessageLevel.ERROR, "Unable to parse message with ip " + ipFieldValue, ipFieldName, FEATURE_NAME);
            log.error("Unable to parse ip in a message {}", extensions);
            return;
        }

        Optional<String> regionNameOptional = regionCidrMap.entrySet().stream()
            .filter(entry -> entry.getKey().getAddress().toIPv6().contains(ipAddressString.getAddress().toIPv6()))
            .min(Comparator.comparing(entry -> entry.getKey().getAddress()
                .getCount())).map(Entry::getValue);

        if (regionNameOptional.isPresent()) {
            enrichment.enrich(extensions, FEATURE_NAME, regionNameOptional.get());
        } else {
            log.debug("Could not find the region for the specified IP. Message: {}", extensions);
        }
    }
}
