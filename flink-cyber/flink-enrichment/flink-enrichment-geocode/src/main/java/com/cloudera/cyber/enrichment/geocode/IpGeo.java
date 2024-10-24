/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.geocode;

import com.cloudera.cyber.Message;
import java.util.List;
import org.apache.flink.streaming.api.datastream.SingleOutputStreamOperator;

public class IpGeo {

    public static SingleOutputStreamOperator<Message> geo(SingleOutputStreamOperator<Message> source,
                                                          List<String> ipFields, String geoDatabasePath) {
        return source
              .map(new IpGeoMap(geoDatabasePath, ipFields, null))
              .name("IP Geo").uid("maxmind-geo");
    }

    public static SingleOutputStreamOperator<Message> asn(SingleOutputStreamOperator<Message> source,
                                                          List<String> ipFields, String asnDatabasePath) {
        return source
              .map(new IpAsnMap(asnDatabasePath, ipFields, null))
              .name("IP ASN").uid("maxmind-asn");
    }
}
