package com.cloudera.cyber.enrichment.geocode.database.types.company;

import java.util.Map;

import static com.cloudera.cyber.enrichment.geocode.database.types.ValueConversions.*;

public class IpInfoCompanyResponseDecoder implements CompanyResponseDecoder {

    public static final String COMPANY_KEY = "name";
    public static final String ASN_KEY = "asn";
    public static final String AS_NAME_KEY = "as_name";

    @Override
    public Object getCompany(Map<String, Object> responseData) {
        return convertEmptyToNull(safeLookup(responseData, COMPANY_KEY, String.class));
    }

    @Override
    public Object getAsnNumber(Map<String, Object> responseData) {
        return extractIpinfoAsnNumber(safeLookup(responseData, ASN_KEY, String.class));
    }

    @Override
    public Object getAutonomousSystemOrganization(Map<String, Object> responseData) {
        return convertEmptyToNull(safeLookup(responseData, AS_NAME_KEY, String.class));
    }
}
