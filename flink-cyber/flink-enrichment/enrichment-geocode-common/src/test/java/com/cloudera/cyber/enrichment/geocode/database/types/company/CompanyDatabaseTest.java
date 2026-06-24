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
import static org.junit.jupiter.api.Assertions.*;

/**
 * Tests for CompanyDatabase.
 * Note: Company database is only supported for IPINFO vendor.
 * MaxMind databases will throw IllegalStateException.
 */
public class CompanyDatabaseTest extends MaxmindDatabaseTest {

    private static final String MAXMIND_ASN_MMDB = "geolite/GeoLite2-ASN-Test.mmdb";
    private static final String INVALID_DATABASE = "geolite/invalid_maxmind_db.mmdb";
    private static final String UNSUPPORTED_EXTENSION = "ipinfo/ipinfo_asn_sample.csv";
    private static final String CLOUDFLARE_IP = "1.1.1.0";
    private static final TestResource resource = new TestResource();

    @TempDir
    Path tempDir;

    @Test
    void testMaxmindDatabaseThrowsUnsupportedVendorException() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        
        assertThatThrownBy(() -> new CompanyDatabase(maxmindPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Company database is only supported for IPINFO vendor");
    }

    @Test
    void testInvalidDatabaseThrowsException() throws URISyntaxException {
        String invalidPath = resource.getFilePath(INVALID_DATABASE);
        
        assertThatThrownBy(() -> new CompanyDatabase(invalidPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Could not read geocode database");
    }

    @Test
    void testUnsupportedExtensionThrowsException() throws URISyntaxException {
        String unsupportedPath = resource.getFilePath(UNSUPPORTED_EXTENSION);
        
        assertThatThrownBy(() -> new CompanyDatabase(unsupportedPath))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("unsupported extension");
    }

    @Test
    void testGetCompanyWithNullResponseReturnsNull() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        try (CompanyDatabase database = new CompanyDatabase(maxmindPath)) {
            assertThatThrownBy(() -> database.getCompany(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Company database is only supported for IPINFO vendor");
        }
    }

    @Test
    void testGetAsnNumberWithNullResponseReturnsNull() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        try (CompanyDatabase database = new CompanyDatabase(maxmindPath)) {
            assertThatThrownBy(() -> database.getAsnNumber(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Company database is only supported for IPINFO vendor");
        }
    }

    @Test
    void testGetAutonomousSystemOrganizationWithNullResponseReturnsNull() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        try (CompanyDatabase database = new CompanyDatabase(maxmindPath)) {
            assertThatThrownBy(() -> database.getAutonomousSystemOrganization(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Company database is only supported for IPINFO vendor");
        }
    }

    @Test
    void testGetNetworkMaskWithNullResponseReturnsNull() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        try (CompanyDatabase database = new CompanyDatabase(maxmindPath)) {
            assertThatThrownBy(() -> database.getNetworkMask(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Company database is only supported for IPINFO vendor");
        }
    }

    @Test
    void testGetOrganizationDelegatesToGetCompany() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        try (CompanyDatabase database = new CompanyDatabase(maxmindPath)) {
            assertThatThrownBy(() -> database.getOrganization(null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Company database is only supported for IPINFO vendor");
        }
    }

    @Test
    void testLookupWithNullIpAddressReturnsNull() throws URISyntaxException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        try (CompanyDatabase database = new CompanyDatabase(maxmindPath)) {
            assertThatThrownBy(() -> database.lookup((InetAddress) null))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Company database is only supported for IPINFO vendor");
        }
    }

    @Test
    void testLookupWithValidIpAddressThrowsForUnsupportedVendor() throws URISyntaxException, IOException {
        String maxmindPath = resource.getFilePath(MAXMIND_ASN_MMDB);
        try (CompanyDatabase database = new CompanyDatabase(maxmindPath)) {
            assertThatThrownBy(() -> database.lookup(InetAddress.getByName(CLOUDFLARE_IP)))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Company database is only supported for IPINFO vendor");
        }
    }
}
