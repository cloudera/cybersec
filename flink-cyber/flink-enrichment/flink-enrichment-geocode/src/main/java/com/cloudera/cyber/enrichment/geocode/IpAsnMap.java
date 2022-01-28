package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.geocode.impl.IpAsnEnrichment;
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
public class IpAsnMap extends RichMapFunction<Message, Message> {
    private final String asnDatabasePath;
    private final List<String> ipFieldNames;
    private transient IpAsnEnrichment asnEnrichment;

    @Override
    public Message map(Message message) {
        Map<String, String> messageFields = message.getExtensions();
        Message newMessage = message;
        List<DataQualityMessage> qualityMessages = new ArrayList<>();
        if (messageFields != null && !ipFieldNames.isEmpty()) {
            Map<String, String> geoExtensions = new HashMap<>();
            for (String ipFieldName : ipFieldNames) {
                Object ipFieldValue = messageFields.get(ipFieldName);
                asnEnrichment.lookup(ipFieldName, ipFieldValue, geoExtensions, qualityMessages);
            }
            newMessage = MessageUtils.enrich(message, geoExtensions, qualityMessages);
        }
        return newMessage;
    }

    @Override
    public void open(Configuration config) {
        try {
            this.asnEnrichment = new IpAsnEnrichment(asnDatabasePath);
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Could not read asn database %s", asnDatabasePath));
        }
    }
}
