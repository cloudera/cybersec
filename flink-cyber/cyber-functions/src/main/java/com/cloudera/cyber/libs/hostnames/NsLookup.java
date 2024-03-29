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

package com.cloudera.cyber.libs.hostnames;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.AbstractStringScalarFunction;
import com.github.benmanes.caffeine.cache.CacheLoader;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.github.benmanes.caffeine.cache.LoadingCache;
import lombok.extern.slf4j.Slf4j;
import org.apache.flink.api.common.typeinfo.TypeInformation;
import org.apache.flink.api.common.typeinfo.Types;
import org.xbill.DNS.Lookup;
import org.xbill.DNS.Record;
import org.xbill.DNS.TextParseException;
import org.xbill.DNS.Type;

import javax.annotation.CheckForNull;
import javax.annotation.Nonnull;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@CyberFunction("nslookup")
@Slf4j
public class NsLookup extends AbstractStringScalarFunction {

    private LoadingCache<NsLookupQuery, List<NsLookupRecord>> cache = Caffeine.newBuilder()
            .expireAfterAccess(60, TimeUnit.SECONDS)
            .maximumSize(1000)
            .build(new CacheLoader<NsLookupQuery, List<NsLookupRecord>>() {
                       @CheckForNull
                       @Override
                       public List<NsLookupRecord> load(@Nonnull NsLookupQuery key) throws Exception {
                           try {
                               Lookup lookup = new Lookup(key.getQuery(), key.getType());
                               Record[] records = lookup.run();
                               if (lookup.getResult() == Lookup.SUCCESSFUL) {
                                   if (records != null) {
                                       return Stream.of(records).map(r -> NsLookupRecord.builder()
                                               .type(Type.string(r.getType()))
                                               .name(r.getName().toString(true))
                                               .result(r.rdataToString()
                                               )
                                               .build()
                                       ).collect(Collectors.toList());
                                   } else {
                                       log.warn("Dns Lookup query='{}' type='{}' returned null records", key.getQuery(), key.getType());
                                       return null;
                                   }
                               } else {
                                   log.error("Dns Lookup query='{}' type='{}' failed with error {}", key.getQuery(), key.getType(), lookup.getErrorString());
                                   return null;
                               }
                           } catch (TextParseException e) {
                               log.error(String.format("Dns Lookup query='%s' type='%s' failed.", key.getQuery(), key.getType()), e);
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
