package com.cloudera.cyber.libs.hostnames;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.AbstractStringScalarFunction;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import org.apache.commons.net.util.SubnetUtils;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.net.InetAddress;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CyberFunction("nslookup")
public class NsLookup extends AbstractStringScalarFunction {
    private LoadingCache<NsLookupQuery, List<NsLookupRecord>> cache = Caffeine.newBuilder()
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build(new CacheLoader<NsLookupQuery, List<NsLookupRecord>>() {
                       @CheckForNull
                       @Override
                       public List<NsLookupRecord> load(@Nonnull NsLookupQuery key) throws Exception {
                           try {
                               Record[] records = new Lookup(key.getQuery(), key.getType()).run();
                               return Stream.of(records).map(r -> NsLookupRecord.builder()
                                       .type(Type.string(r.getType()))
                                       .name(r.getName().toString(true))
                                       .result(r.rdataToString()
                                       )
                                       .build()
                               ).collect(Collectors.toList());
                           } catch (TextParseException e) {
                               return null;
                           }
                       }
                   }
            );

    public List<NsLookupRecord> eval(String q, String type) {
        return cache.get(NsLookupQuery.builder().query(q).type(Type.value(type)).build());
    }

    public TypeInformation<?> getResultType(Class<?>[] signature) {
        return Types.LIST(TypeInformation.of(NsLookupRecord.class));
    }

    @Override
    public boolean isDeterministic() {
        return true;
    }

}
