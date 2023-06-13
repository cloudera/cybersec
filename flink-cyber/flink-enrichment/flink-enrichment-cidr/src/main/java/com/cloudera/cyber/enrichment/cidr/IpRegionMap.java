package com.cloudera.cyber.enrichment.cidr;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.cidr.impl.types.RegionCidrEnrichmentConfiguration;
import com.cloudera.cyber.enrichment.cidr.impl.IpRegionCidrEnrichment;
import inet.ipaddr.IPAddressString;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

@EqualsAndHashCode(callSuper = true)
@Data
@RequiredArgsConstructor
@Slf4j
public class IpRegionMap extends RichMapFunction<Message, Message> {

    private final RegionCidrEnrichmentConfiguration cidrEnrichmentMap;
    private final List<String> ipFieldNames;

    private transient IpRegionCidrEnrichment regionCidrEnrichment;

    @Override
    public Message map(Message message) throws Exception {
        Map<String, String> messageFields = message.getExtensions();
        Message newMessage = message;
        List<DataQualityMessage> qualityMessages = new ArrayList<>();
        if (messageFields != null && !ipFieldNames.isEmpty()) {
            Map<String, String> extensions = new HashMap<>();
            for (String ipFieldName : ipFieldNames) {
                Optional.ofNullable(messageFields.get(ipFieldName))
                    .ifPresent(ipFieldValue -> regionCidrEnrichment.lookup(ipFieldName, ipFieldValue, extensions, qualityMessages ));
            }
            newMessage = MessageUtils.enrich(message, extensions, qualityMessages);
        }
        return newMessage;
    }

    @Override
    public void open(Configuration parameters) throws Exception {
        super.open(parameters);
        Map<IPAddressString,String> regionCidrMap = new HashMap<>();
        for (Map<String, List<String>> regionCidrs : cidrEnrichmentMap.values()) {
            for (Entry<String, List<String>> regionCidrEntry : regionCidrs.entrySet()) {
                for (String cidr : regionCidrEntry.getValue()) {
                    regionCidrMap.put(new IPAddressString(cidr), regionCidrEntry.getKey());
                }
            }
        }
        regionCidrEnrichment = new IpRegionCidrEnrichment(regionCidrMap);
    }
}
