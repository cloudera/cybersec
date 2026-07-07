package com.cloudera.cyber.enrichment.geocode.database;

import com.cloudera.cyber.enrichment.geocode.database.types.asn.AsnDatabaseResponseDecoder;
import com.cloudera.cyber.enrichment.geocode.database.types.asn.IpInfoAsnResponseDecoder;
import com.cloudera.cyber.enrichment.geocode.database.types.asn.MaxmindAsnResponseDecoder;
import com.cloudera.cyber.enrichment.geocode.database.types.company.CompanyResponseDecoder;
import com.cloudera.cyber.enrichment.geocode.database.types.company.IpInfoCompanyResponseDecoder;
import com.cloudera.cyber.enrichment.geocode.database.types.geo.GeoDatabaseResponseDecoder;
import com.cloudera.cyber.enrichment.geocode.database.types.geo.IpInfoGeoResponseDecoder;
import com.cloudera.cyber.enrichment.geocode.database.types.geo.MaxmindGeoResponseDecoder;
import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public enum MaxmindDatabaseVendor {
    MAXMIND(new MaxmindAsnResponseDecoder(), new MaxmindGeoResponseDecoder(), null),
    IPINFO(new IpInfoAsnResponseDecoder(), new IpInfoGeoResponseDecoder(), new IpInfoCompanyResponseDecoder());

    private final AsnDatabaseResponseDecoder asnDecoder;
    private final GeoDatabaseResponseDecoder geoDecoder;
    private final CompanyResponseDecoder companyDecoder;
}
