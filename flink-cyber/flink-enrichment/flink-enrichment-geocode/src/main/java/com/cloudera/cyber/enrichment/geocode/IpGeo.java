package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import org.apache.flink.streaming.api.datastream.DataStream;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

import java.util.List;

public class IpGeo {

    public static SingleOutputStreamOperator<Message> geo(DataStream<Message> source, List<String> ipFields, String geoDatabasePath) {
        return source
                .map(new IpGeoMap(geoDatabasePath, ipFields, null))
                .name("IP Geo").uid("maxmind-geo");
    }

    public static DataStream<Message> asn(DataStream<Message> source, List<String> ipFields, String asnDatabasePath) {
        return source
                .map(new IpAsnMap(asnDatabasePath, ipFields, null))
                .name("IP ASN").uid("maxmind-asn");
    }
}
