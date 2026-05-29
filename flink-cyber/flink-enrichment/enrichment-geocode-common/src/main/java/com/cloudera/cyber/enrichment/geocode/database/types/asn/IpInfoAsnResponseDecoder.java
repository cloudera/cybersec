package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import java.util.Map;

public class IpInfoAsnResponseDecoder implements AsnDatabaseResponseDecoder {
    public static final String AS_ORG_KEYWORD = "name";
    public static final String AS_NUM_KEYWORD = "asn";

    @Override
    public Object getAsnNumber(Map<String, Object> response) {
        if (response != null && response.get(AS_NUM_KEYWORD) instanceof String asnAsString && asnAsString.matches("AS[0-9]*")) {
            return Long.valueOf(asnAsString.substring(2));
        }
        else {
            return null;
        }
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
