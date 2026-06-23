package com.cloudera.cyber.enrichment.geocode.database.types.company;

import java.util.Map;

public interface CompanyResponseDecoder {

    Object getCompany(Map<String, Object> responseData);

    Object getAsnNumber(Map<String, Object> responseData);

    Object getAutonomousSystemOrganization(Map<String, Object> responseData);

}
