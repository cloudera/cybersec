package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import java.util.Map;

import static com.cloudera.cyber.enrichment.geocode.database.types.ValueConversions.*;

public class IpInfoAsnResponseDecoder implements AsnDatabaseResponseDecoder {
    public static final String AS_ORG_KEYWORD = "name";
    public static final String AS_NUM_KEYWORD = "asn";

    @Override
    public Object getAsnNumber(Map<String, Object> response) {
        return extractIpinfoAsnNumber(safeLookup(response, AS_NUM_KEYWORD, String.class));
    }

    @Override
    public Object getAutonomousSystemOrganization(Map<String, Object> response) {
        return convertEmptyToNull(safeLookup(response, AS_ORG_KEYWORD, String.class));
    }
}
