package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.api.java.ExecutionEnvironment;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.util.*;

@EqualsAndHashCode(callSuper = true)
@Data
@AllArgsConstructor
public class IpGeoMap extends RichMapFunction<Message, Message> {
    public static final String GEOCODE_FEATURE = "geo";
    public static final IpGeoEnrichment.GeoEnrichmentFields[] AGGREGATE_GEO_FIELDS =
            new IpGeoEnrichment.GeoEnrichmentFields[]{IpGeoEnrichment.GeoEnrichmentFields.CITY, IpGeoEnrichment.GeoEnrichmentFields.COUNTRY};
    public static final IpGeoEnrichment.GeoEnrichmentFields[] ALL_GEO_FIELDS = IpGeoEnrichment.GeoEnrichmentFields.values();

    private final String geocodeDatabasePath;
    private final List<String> ipFieldNames;
    private transient IpGeoEnrichment geoEnrichment;

    @Override
    public Message map(Message message) {
        Map<String, Object> messageFields = message.getExtensions();
        Message newMessage = message;
        List<DataQualityMessage> qualityMessages = new ArrayList<>();
        if (messageFields != null && !ipFieldNames.isEmpty()) {
            Map<String, Object> geoExtensions = new HashMap<>();
            for (String ipFieldName : ipFieldNames) {
                Object ipFieldValue = messageFields.get(ipFieldName);
                geoEnrichment.lookup(ipFieldName, ipFieldValue, getGeoFieldSet(ipFieldValue), geoExtensions, qualityMessages);
            }
            newMessage = MessageUtils.enrich(message, geoExtensions, qualityMessages);
        }
        return newMessage;
    }

    private IpGeoEnrichment.GeoEnrichmentFields[] getGeoFieldSet(Object ipFieldValue) {
        if (ipFieldValue instanceof Collection) {
            return AGGREGATE_GEO_FIELDS;
        } else {
            return ALL_GEO_FIELDS;
        }
    }

    @Override
    public void open(Configuration config) throws Exception {
        ExecutionEnvironment.getExecutionEnvironment().readTextFile(geocodeDatabasePath);

        FileSystem fileSystem = new Path(geocodeDatabasePath).getFileSystem();
        try (FSDataInputStream dbStream = fileSystem.open(new Path(geocodeDatabasePath))) {
            this.geoEnrichment = new IpGeoEnrichment(new DatabaseReader.Builder(dbStream).withCache(new CHMCache()).build());
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Could not load geocode database file '%s'.", geocodeDatabasePath), e);
        }
    }
}
