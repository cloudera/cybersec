package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import java.util.Map;

public class MaxmindAsnResponseDecoder implements AsnDatabaseResponseDecoder {
    public static final String AS_ORG_KEYWORD = "autonomous_system_organization";
    public static final String AS_NUM_KEYWORD = "autonomous_system_number";

    @Override
    public Object getAsnNumber(Map<String, Object> response) {
        if (response != null) {
            if (response.get(AS_NUM_KEYWORD) instanceof Long asNumber) {
                return asNumber;
            }
        }
        return null;
    }

    @Override
    public Object getAutonomousSystemOrganization(Map<String, Object> response) {
        if (response != null) {
            return response.get(AS_ORG_KEYWORD);
        } else {
            return null;
        }
    }
}
