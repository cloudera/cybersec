package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.geocode.impl.IpAsnEnrichment;
import com.maxmind.db.CHMCache;
import com.maxmind.geoip2.DatabaseReader;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.functions.RichMapFunction;
import org.apache.flink.configuration.Configuration;
import org.apache.flink.core.fs.FSDataInputStream;
import org.apache.flink.core.fs.FileSystem;
import org.apache.flink.core.fs.Path;

import java.io.IOException;
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

    private static IpAsnEnrichment loadEnrichment(String asnDatabasePath)  {
        log.info("Loading Maxmind database {}", asnDatabasePath);
        IpAsnEnrichment asnEnrichment = null;
        try {
            FileSystem fileSystem = new Path(asnDatabasePath).getFileSystem();

            try (FSDataInputStream dbStream = fileSystem.open(new Path(asnDatabasePath))) {
                asnEnrichment = new IpAsnEnrichment(new DatabaseReader.Builder(dbStream).withCache(new CHMCache()).build());
                log.info("Successfully loaded Maxmind database {}", asnDatabasePath);
            } catch (Exception e) {
                log.error(String.format("Exception while loading geocode database %s", asnDatabasePath), e);
            }
        } catch (IOException ioe) {
            log.error(String.format("Unable to load file system %s", asnDatabasePath), ioe);
        }

        return asnEnrichment;
    }

    @Override
    public void open(Configuration config) {
        this.asnEnrichment = IpAsnMap.loadEnrichment(asnDatabasePath);
        if (this.asnEnrichment == null) {
            throw new IllegalStateException(String.format("Could not read asn database %s", asnDatabasePath));
        }
    }
}
