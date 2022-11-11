package com.cloudera.cyber.enrichment.hbase;

import com.cloudera.cyber.hbase.LookupKey;
import lombok.*;
import lombok.experimental.SuperBuilder;
import org.apache.hadoop.hbase.client.Get;
import org.apache.hadoop.hbase.client.Result;
import org.apache.hadoop.hbase.util.Bytes;

import java.util.HashMap;
import java.util.Map;

@Getter
@SuperBuilder
@EqualsAndHashCode(callSuper = true)
public class SimpleLookupKey extends LookupKey {

    @Override
    public Get toGet() {
        Get get = new Get(Bytes.toBytes(getKey()));
        get.addFamily(Bytes.toBytes(getCf()));

        return get;
    }

    @Override
    public Map<String, Object> resultToMap(Result result) {
        Map<String, Object> hbaseMap = new HashMap<>();
        result.getFamilyMap(Bytes.toBytes(getCf())).forEach((k,v) -> hbaseMap.put( Bytes.toString(k), Bytes.toString(v)));

        return hbaseMap;
    }
}
