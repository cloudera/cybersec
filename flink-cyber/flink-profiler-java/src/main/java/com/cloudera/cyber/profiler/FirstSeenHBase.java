package com.cloudera.cyber.profiler;

import com.cloudera.cyber.Message;
import com.cloudera.cyber.enrichment.hbase.LookupKey;
import com.cloudera.cyber.profiler.accumulator.ProfileGroupAcc;
import lombok.Data;
import org.apache.hadoop.hbase.util.Bytes;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Data
public class FirstSeenHBase implements Serializable {
    private static final long serialVersionUID = 1L;

    private String tableName;
    private byte[] columnFamilyName;
    private String profileName;
    private List<String> keyFieldNames;
    private String firstSeenResultName;

    public FirstSeenHBase(String tableName, String columnFamilyName, ProfileGroupConfig profileGroupConfig) {
        this.tableName = tableName;
        this.columnFamilyName = Bytes.toBytes(columnFamilyName);
        this.profileName = profileGroupConfig.getProfileGroupName();
        this.keyFieldNames = profileGroupConfig.getKeyFieldNames();
        ProfileMeasurementConfig measurementConfig = profileGroupConfig.getMeasurements().stream().filter(m -> m.getAggregationMethod().equals(ProfileAggregationMethod.FIRST_SEEN)).
                findFirst().orElseThrow(() -> new NullPointerException("Expected at least one first seen measurement but none was found."));
        this.firstSeenResultName = measurementConfig.getResultExtensionName();
    }

    public LookupKey getKey(Message message) {
        Map<String, String> extensions = message.getExtensions();
        String key = Stream.concat(Stream.of(profileName),
                keyFieldNames.stream().map(extensions::get)).collect(Collectors.joining(":"));
        return LookupKey.builder()
                .key(Bytes.toBytes(key))
                .cf(columnFamilyName)
                .build();
    }

    public String getFirstSeen(Message message) {
        return message.getExtensions().get(ProfileGroupAcc.START_PERIOD_EXTENSION);
    }

    public String getLastSeen(Message message) {
        return message.getExtensions().get(ProfileGroupAcc.END_PERIOD_EXTENSION);
    }

}
