package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.GeoEnrichmentFields;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
@Slf4j
public class IpGeoMap extends RichMapFunction<Message, Message> {
    public static final String GEOCODE_FEATURE = "geo";
    public static final GeoEnrichmentFields[] AGGREGATE_GEO_FIELDS =
            new GeoEnrichmentFields[]{GeoEnrichmentFields.CITY, GeoEnrichmentFields.COUNTRY};
    public static final GeoEnrichmentFields[] ALL_GEO_FIELDS = GeoEnrichmentFields.values();
    private final String geocodeDatabasePath;
    private final List<String> ipFieldNames;
    private transient IpGeoEnrichment geoEnrichment;

    @Override
    public Message map(Message message) {
        Map<String, String> messageFields = message.getExtensions();
        Message newMessage = message;
        List<DataQualityMessage> qualityMessages = new ArrayList<>();
        if (messageFields != null && !ipFieldNames.isEmpty()) {
            Map<String, String> geoExtensions = new HashMap<>();
            for (String ipFieldName : ipFieldNames) {
                Object ipFieldValue = messageFields.get(ipFieldName);
                geoEnrichment.lookup(ipFieldName, ipFieldValue, getGeoFieldSet(ipFieldValue), geoExtensions, qualityMessages);
            }
            newMessage = MessageUtils.enrich(message, geoExtensions, qualityMessages);
        }
        return newMessage;
    }

    private GeoEnrichmentFields[] getGeoFieldSet(Object ipFieldValue) {
        if (ipFieldValue instanceof Collection) {
            return AGGREGATE_GEO_FIELDS;
        } else {
            return ALL_GEO_FIELDS;
        }
    }

    @Override
    public void open(Configuration config) {
        this.geoEnrichment = new IpGeoEnrichment(geocodeDatabasePath);
    }
}
