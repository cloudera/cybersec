package com.cloudera.cyber.enrichment.geocode.database.types.company;

import com.cloudera.cyber.enrichment.geocode.database.types.MaxmindDatabaseTest;
import com.cloudera.cyber.enrichment.geocode.database.types.TestResource;
import org.junit.jupiter.api.Test;

import java.net.URISyntaxException;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
    private static final TestResource resource = new TestResource();

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
    void testConstructorWithNullPathThrowsException() {
        assertThatThrownBy(() -> new CompanyDatabase(null))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void testConstructorWithBlankPathThrowsException() {
        assertThatThrownBy(() -> new CompanyDatabase("   "))
                .isInstanceOf(IllegalArgumentException.class);
    }
}
