package com.cloudera.cyber.enrichment.threatq;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.MessageUtils;
import com.cloudera.cyber.enrichment.hbase.AbstractHbaseMapFunction;
import com.cloudera.cyber.enrichment.hbase.LookupKey;
import lombok.extern.slf4j.Slf4j;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static java.util.stream.Collectors.toMap;

@Slf4j
public class ThreatQHBaseMap extends AbstractHbaseMapFunction {
    private static final byte[] cf = Bytes.toBytes("t");

    private List<ThreatQConfig> configs;

    public ThreatQHBaseMap(List<ThreatQConfig> configs) {
        super();
        this.configs = configs;
        log.info("Configuration: {}", configs);
    }

    @Override
    public Message map(Message message) {
        if (this.configs == null) return message;

        Map<String, String> results = configs.stream()
                .map(config -> {
                    String f = config.getField();
                    if (!message.getExtensions().containsKey(f)) {
                        return Collections.<String,String>emptyMap();
                    }
                    String k = config.getIndicatorType() + ":" + message.getExtensions().get(f);
                    LookupKey key = LookupKey.builder()
                            .cf(cf)
                            .key(Bytes.toBytes(k)).build();
                    return hbaseLookup(message.getTs(), key, f + ".threatq");
                })
                .flatMap(m -> m.entrySet().stream())
                .collect(toMap(k -> k.getKey(), v -> v.getValue()));

        return MessageUtils.addFields(message, results);
    }

    @Override
    protected String getTableName() {
        return "threatq";
    }
}
