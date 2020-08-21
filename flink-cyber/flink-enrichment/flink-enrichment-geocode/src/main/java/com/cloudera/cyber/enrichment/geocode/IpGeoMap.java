package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.data.quality.MessageLevel;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import com.maxmind.geoip2.DatabaseReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.apache.commons.validator.routines.InetAddressValidator;
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
    public static final String FIELD_VALUE_IS_NOT_A_STRING = "'%s' is not a String.";
    public static final String FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS = "'%s' is not a valid IP address.";

    private final String geocodeDatabasePath;
    private final List<String> ipFieldNames;
    private transient IpGeoEnrichment geoEnrichment;

    @Override
    public Message map(Message message) {
        for (String ipFieldName : ipFieldNames) {
            Map<String, Object> messageFields = message.getExtensions();
            Object ipFieldValue = messageFields.get(ipFieldName);
            if (ipFieldValue instanceof Collection<?>) {
                ((Collection<?>) ipFieldValue).forEach((ipObject) ->
                        lookup(message, ipFieldName, ipObject, AGGREGATE_GEO_FIELDS).
                                forEach((geoField, enrichmentValue) ->
                                        MessageUtils.enrichSet(message,ipFieldName, GEOCODE_FEATURE, geoField.getPluralName(), enrichmentValue)));
            } else if (ipFieldValue != null){
                lookup(message, ipFieldName, ipFieldValue, IpGeoEnrichment.GeoEnrichmentFields.values()).forEach(
                        (enrichmentType, enrichmentValue) ->
                                MessageUtils.enrich(message, ipFieldName, GEOCODE_FEATURE, enrichmentType.getSingularName(), enrichmentValue));
            }
        }

        return message;
    }

    private Map<IpGeoEnrichment.GeoEnrichmentFields, Object> lookup(Message message, String fieldName, Object ipObject,
                                                                    IpGeoEnrichment.GeoEnrichmentFields[] geoFields) {
        if (ipObject instanceof String) {
            try {
                String ipString = (String)ipObject;
                if (InetAddressValidator.getInstance().isValid(ipString)) {
                    return geoEnrichment.lookup(ipString, geoFields);
                } else {
                    MessageUtils.reportQualityMessage(message, MessageLevel.INFO, fieldName, GEOCODE_FEATURE, String.format(FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, ipString));
                }
            } catch (Exception e) {
                MessageUtils.reportQualityMessage(message, MessageLevel.INFO, fieldName, GEOCODE_FEATURE, String.format("Geocode failed '%s'", e.getMessage()));
            }
        } else {
            MessageUtils.reportQualityMessage(message, MessageLevel.INFO, fieldName, GEOCODE_FEATURE, String.format(FIELD_VALUE_IS_NOT_A_STRING, ipObject.toString()));
        }

        return Collections.emptyMap();
    }

    @Override
    public void open(Configuration config) throws Exception {
        ExecutionEnvironment.getExecutionEnvironment().readTextFile(geocodeDatabasePath);

        FileSystem fileSystem = new Path(geocodeDatabasePath).getFileSystem();
        try (FSDataInputStream dbStream = fileSystem.open(new Path(geocodeDatabasePath))) {
            this.geoEnrichment = new IpGeoEnrichment(new DatabaseReader.Builder(dbStream).build());
        } catch (Exception e) {
            throw new IllegalStateException(String.format("Could not load geocode database file '%s'.", geocodeDatabasePath), e);
        }
    }
}
