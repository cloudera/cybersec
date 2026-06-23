package com.cloudera.cyber.enrichment.geocode.database.types.company;

import java.util.Map;

public class IpInfoCompanyResponseDecoder implements CompanyResponseDecoder {

    public static final String COMPANY_KEY = "name";
    public static final String ASN_KEY = "asn";
    public static final String AS_NAME_KEY = "as_name";

    @Override
    public Object getCompany(Map<String, Object> responseData) {
        if (responseData != null) {
            return responseData.get(COMPANY_KEY);
        }
        return null;
    }

    @Override
    public Object getAsnNumber(Map<String, Object> responseData) {
        if (responseData != null && responseData.get(ASN_KEY) instanceof String asnAsString && asnAsString.matches("AS[0-9]*")) {
            return Long.valueOf(asnAsString.substring(2));
        }
        return null;
    }

    @Override
    public Object getAutonomousSystemOrganization(Map<String, Object> responseData) {
        if (responseData != null) {
            return responseData.get(AS_NAME_KEY);
        }
        return null;
    }
}
