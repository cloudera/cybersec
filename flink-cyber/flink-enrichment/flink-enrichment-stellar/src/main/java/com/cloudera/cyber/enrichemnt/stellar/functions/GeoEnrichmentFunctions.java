/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.cloudera.cyber.enrichemnt.stellar.functions;

import com.cloudera.cyber.enrichment.MetronGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.IpGeoJob;
import com.cloudera.cyber.enrichment.geocode.impl.IpAsnEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.IpGeoEnrichment;
import com.cloudera.cyber.enrichment.geocode.impl.types.GeoFields;
import com.cloudera.cyber.enrichment.geocode.impl.types.MetronGeoEnrichmentFields;
import com.google.common.collect.ImmutableMap;
import lombok.AccessLevel;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.MapUtils;
import org.apache.metron.stellar.dsl.Context;
import org.apache.metron.stellar.dsl.ParseException;
import org.apache.metron.stellar.dsl.Stellar;
import org.apache.metron.stellar.dsl.StellarFunction;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@NoArgsConstructor(access = AccessLevel.PRIVATE)
public class GeoEnrichmentFunctions {

    private static final Map<String,String> ASN_RESULT_TO_METRON_KEYS = ImmutableMap.of("org", "autonomous_system_organization",
            "number", "autonomous_system_number",
            "mask","network");
    @Stellar(name = "GET"
            , namespace = "GEO"
            , description = "Look up an IPV4 address and returns geographic information about it"
            , params = {
            "ip - The IPV4 address to lookup",
            "fields - Optional list of GeoIP fields to grab. Options are locID, country, city, postalCode, dmaCode, latitude, longitude, location_point"
    }
            , returns = "If a Single field is requested a string of the field, If multiple fields a map of string of the fields, and null otherwise"
    )
    public static class GeoGet implements StellarFunction {
        boolean initialized = false;
        private IpGeoEnrichment ipGeoEnrichment;

        @Override
        public Object apply(List<Object> args, Context context) throws ParseException {
            if (!isInitialized()) {
                return null;
            }
            if (args.size() > 2) {
                throw new IllegalArgumentException("GEO_GET received more arguments than expected: " + args.size());
            }
            HashMap<String, String> result = new HashMap<>();
            if (args.size() == 1 && args.get(0) instanceof String) {
                // If no fields are provided, return everything
                String ip = (String) args.get(0);
                if (ip == null || ip.trim().isEmpty()) {
                    return null;
                }
                ipGeoEnrichment.lookup(MetronGeoEnrichment::new, null, ip, MetronGeoEnrichmentFields.values(), result, Collections.emptyList());

                return result;
            } else if (args.size() == 2 && args.get(1) instanceof List) {
                // If fields are provided, return just those fields.
                String ip = (String) args.get(0);
                @SuppressWarnings("unchecked")
                GeoFields[] geoFieldSet = ((List<String>) args.get(1)).stream().map(MetronGeoEnrichmentFields::fromSingularName).toArray(GeoFields[]::new);
                ipGeoEnrichment.lookup(MetronGeoEnrichment::new, null, ip, geoFieldSet, result, Collections.emptyList());
                if (geoFieldSet.length == 1) {
                    if (MapUtils.isNotEmpty(result)) {
                        return result.values().iterator().next();
                    } else {
                        return null;
                    }
                }
                return result;
            }
            return null;
        }

        @Override
        public void initialize(Context context) {
            log.info("Initializing GEO_GET function");
            Map<String, Object> config = getConfig(context);
            String databasePath = (String) config.get(IpGeoJob.PARAM_GEO_DATABASE_PATH);
            ipGeoEnrichment = new IpGeoEnrichment(databasePath);
            initialized = true;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> getConfig(Context context) {
            return (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG, false)
                    .orElse(new HashMap<>());
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }
    }


    @Stellar(name = "GET"
            , namespace = "ASN"
            , description = "Look up an IPV4 address and returns Autonomous System Number information about it"
            , params = {
            "ip - The IPV4 address to lookup",
            "fields - Optional list of ASN fields to grab. Options are autonomous_system_organization, autonomous_system_number, network"
    }
            , returns = "If a single field is requested a string of the field, If multiple fields a map of string of the fields, and null otherwise"
    )
    public static class AsnGet implements StellarFunction {

        boolean initialized = false;
        private IpAsnEnrichment ipAsnEnrichment;

        @Override
        public Object apply(List<Object> args, Context context) throws ParseException {
            if (!isInitialized()) {
                return null;
            }
            if (args.size() > 2) {
                throw new IllegalArgumentException(
                        "ASN_GET received more arguments than expected: " + args.size());
            }
            HashMap<String, String> result = new HashMap<>();

            if (args.size() == 1 && args.get(0) instanceof String) {
                // If no fields are provided, return everything
                String ip = (String) args.get(0);
                if (ip == null || ip.trim().isEmpty()) {
                    return null;
                }
                ipAsnEnrichment.lookup(MetronGeoEnrichment::new, null, ip, result, null);

                return convertToMetronKeys(result);
            } else if (args.size() == 2 && args.get(1) instanceof List) {
                // If fields are provided, return just those fields.
                String ip = (String) args.get(0);
                @SuppressWarnings("unchecked")
                List<String> fields = (List) args.get(1);
                ipAsnEnrichment.lookup(MetronGeoEnrichment::new, null, ip, result, null);
                result = convertToMetronKeys(result);
                // If only one field is requested, just return it directly
                if (fields.size() == 1 && MapUtils.isNotEmpty(result)) {
                    if (!result.containsKey(fields.get(0))) {
                        return null;
                    }
                    return result.get(fields.get(0));
                } else if (MapUtils.isNotEmpty(result)) {
                    // If multiple fields are requested, return all of them
                    return result.entrySet().stream().filter(entry -> fields.contains(entry.getKey())).collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
                }
            }

            return null;
        }

        private static HashMap<String, String> convertToMetronKeys(Map<String, String> result) {
            return result.entrySet().stream().collect(Collectors.toMap(e -> ASN_RESULT_TO_METRON_KEYS.getOrDefault(e.getKey(), e.getKey()), Map.Entry::getValue, (prev, next) -> next, HashMap::new));
        }

        @Override
        public void initialize(Context context) {
            log.info("Initializing ASN_GET function");
            Map<String, Object> config = getConfig(context);
            String databasePath = (String) config.get(IpGeoJob.PARAM_ASN_DATABASE_PATH);
            ipAsnEnrichment = new IpAsnEnrichment(databasePath);
            initialized = true;
        }

        @SuppressWarnings("unchecked")
        private static Map<String, Object> getConfig(Context context) {
            return (Map<String, Object>) context.getCapability(Context.Capabilities.GLOBAL_CONFIG, false)
                    .orElse(new HashMap<>());
        }

        @Override
        public boolean isInitialized() {
            return initialized;
        }

    }
}
