package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import java.util.Map;

public interface AsnDatabaseResponseDecoder {

    Object getAsnNumber(Map<String, Object> responseData);

    Object getAutonomousSystemOrganization(Map<String, Object> responseData);

}
