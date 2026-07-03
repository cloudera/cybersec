package com.cloudera.cyber.enrichment.geocode.database.types.company;

import com.cloudera.cyber.enrichment.geocode.database.types.MaxmindDatabaseTest;
import com.cloudera.cyber.enrichment.geocode.database.types.TestResource;
import com.maxmind.db.DatabaseRecord;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Tests for CompanyDatabase.
 * Note: Company database is only supported for IPINFO vendor.
 * MaxMind databases will throw IllegalStateException.
 * Tests requiring a valid IPINFO company database would need a test database to be added.
 */
public class CompanyDatabaseTest extends MaxmindDatabaseTest {

    private static final String MAXMIND_ASN_MMDB = "geolite/GeoLite2-ASN-Test.mmdb";
    private static final String INVALID_DATABASE = "geolite/invalid_maxmind_db.mmdb";
    private static final String UNSUPPORTED_EXTENSION = "ipinfo/ipinfo_asn_sample.csv";
    private static final String IPINFO_COMPANY_MMDB = "ipinfo/ip_company_sample.mmdb";
    private static final String IPINFO_COMPANY_MMDB_GZ = "ipinfo/ip_company_sample.mmdb.gz";
    private static final String CLOUDFLARE_DNS_IP = "1.1.1.0";
    private static final String CLOUDFLARE_NETWORK = "1.1.1.0/24";
    private static final long CLOUDFLARE_ASN = 13335;
    private static final String CLOUDFLARE_COMPANY_NAME = "APNIC and Cloudflare DNS Resolver project";
    private static final String CLOUDFLARE_AS_ORG = "Cloudflare, Inc.";
    private static final String IP_WITH_NO_INFO = "1.25.0.0";
    private static final String IP_WITH_NO_ASN = "1.0.17.0";
    private static final String IP_WITH_NO_ASN_COMPANY_NAME = "i2ts inc.";

    private static final TestResource resource = new TestResource();

    @TempDir
    Path tempDir;

    @Test
    void testIpinfo() throws URISyntaxException, IOException {
        String ipinfoCompanyPath = resource.getFilePath(IPINFO_COMPANY_MMDB);

        testIpInfo(ipinfoCompanyPath);
        testIpInfo(resource.getFilePath(IPINFO_COMPANY_MMDB_GZ));
        testIpInfo(createTarGzWithMmdb(tempDir, ipinfoCompanyPath));
    }

    private void testIpInfo(String companyPath) throws IOException {
        try (CompanyDatabase companyDatabase = new CompanyDatabase(companyPath)) {
            verifyCompanyLookup(companyDatabase, CLOUDFLARE_DNS_IP, CLOUDFLARE_NETWORK,
                    CLOUDFLARE_COMPANY_NAME, CLOUDFLARE_ASN, CLOUDFLARE_AS_ORG);

            verifyCompanyLookup(companyDatabase, IP_WITH_NO_ASN, "1.0.17.0/24",
                    IP_WITH_NO_ASN_COMPANY_NAME, null, null);

            testNoInfoAndNull(companyDatabase);
        }
    }

    private void verifyCompanyLookup(CompanyDatabase companyDatabase, String ip, String network, String companyName, Long asnNumber, String asnOrganization) throws IOException {
        //noinspection rawtypes
        DatabaseRecord<Map> response = companyDatabase.lookup(InetAddress.getByName(ip));
        assertEquals(companyName, companyDatabase.getCompany(response));
        assertEquals(asnNumber, companyDatabase.getAsnNumber(response));
        assertEquals(asnOrganization, companyDatabase.getAutonomousSystemOrganization(response));
        assertEquals(network, companyDatabase.getNetworkMask(response));
    }

    private void testNoInfoAndNull(CompanyDatabase database) throws IOException {

        // check ip with no info
        assertNull(database.lookup(InetAddress.getByName(IP_WITH_NO_INFO)));

        // check null address
        assertNull(database.lookup(null));

        // check null responses return null values
        assertNull(database.getCompany(null));
        assertNull(database.getAsnNumber(null));
        assertNull(database.getNetworkMask(null));
        assertNull(database.getAutonomousSystemOrganization(null));
    }

    @Test
    void testMaxmindDatabaseThrowsUnsupportedVendorException() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);

        //noinspection resource
        assertThatThrownBy(() -> new CompanyDatabase(maxmindPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Company database is only supported for IPINFO vendor");
    }

    @Test
    void testInvalidDatabaseThrowsException() throws URISyntaxException {
        String invalidPath = resource.getFilePath(INVALID_DATABASE);

        //noinspection resource
        assertThatThrownBy(() -> new CompanyDatabase(invalidPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read geocode database");
    }

    @Test
    void testUnsupportedExtensionThrowsException() throws URISyntaxException {
        String unsupportedPath = resource.getFilePath(UNSUPPORTED_EXTENSION);

        //noinspection resource
        assertThatThrownBy(() -> new CompanyDatabase(unsupportedPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsupported extension");
    }

    @Test
    void testConstructorWithNullPathThrowsException() {
        //noinspection resource
        assertThatThrownBy(() -> new CompanyDatabase(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConstructorWithBlankPathThrowsException() {
        //noinspection resource
        assertThatThrownBy(() -> new CompanyDatabase("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
