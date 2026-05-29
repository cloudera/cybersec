package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import com.cloudera.cyber.enrichment.geocode.database.types.TestResource;
import com.maxmind.db.DatabaseRecord;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.net.InetAddress;
import java.net.URISyntaxException;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;

public class AsnDatabaseTest {

    private static final String IPINFO_ASN_MMDB = "ipinfo/ipinfo_asn_sample.mmdb";
    private static final String IPINFO_ASN_MMDB_GZ = "ipinfo/ipinfo_asn_sample.mmdb.gz";
    private static final String IPINFO_ASN_MMDB_TAR_GZ = "ipinfo/ipinfo_asn_sample.tar.gz";
    private static final String IPINFO_NO_MMDB_TAR = "ipinfo/ipinfo_no_mmdb.tar.gz";
    private static final String MAXMIND_ASN_MMDB = "geolite/GeoLite2-ASN-Test.mmdb";
    private static final String MAXMIND_ASN_MMDB_GZ = "geolite/GeoLite2-ASN-Test.mmdb.gz";
    private static final String MAXMIND_ASN_TAR_GZ = "geolite/GeoLite2-ASN-Test.tar.gz";
    private static final String UNSUPPORTED_EXTENSION = "ipinfo/ipinfo_asn_sample.csv";
    private static final String CLOUDFLARE_IP = "1.1.1.0";
    private static final String IP_WITH_NO_INFO = "1.2.0.0";
    private static final String TELSTRA_IP_V4 = "1.128.0.0";
    private static final String SUNRISE_IP_V6 = "2001:1700:0000:0000:0000:0000:0000:0000";
    private static final TestResource resource = new TestResource();

    @Test
    public void testIpInfo() throws IOException, URISyntaxException {
        testIpInfo(IPINFO_ASN_MMDB);
        testIpInfo(IPINFO_ASN_MMDB_GZ);
        //testIpInfo(IPINFO_ASN_MMDB_TAR_GZ);
    }

    private void testIpInfo(String path) throws URISyntaxException, IOException {
        AsnDatabase database = new AsnDatabase(resource.getFilePath(path));

        verifyAsnLookup(database, CLOUDFLARE_IP, "1.1.1.0/24",13335L, "Cloudflare, Inc.");

        testNoInfoAndNull(database);
    }

    private void testNoInfoAndNull(AsnDatabase database) throws IOException {

        // check ip with no info
        assertNull(database.lookup(InetAddress.getByName(IP_WITH_NO_INFO)));

        // check null address
        assertNull(database.lookup(null));

        // check null responses return null values
        assertNull(database.getAsnNumber(null));
        assertNull(database.getNetworkMask(null));
        assertNull(database.getAutonomousSystemOrganization(null));
    }

    @Test
    public void testMaxmind() throws URISyntaxException, IOException {
        testMaxmind(MAXMIND_ASN_MMDB);
        testMaxmind(MAXMIND_ASN_MMDB_GZ);
        //testMaxmind(MAXMIND_ASN_TAR_GZ);
    }

    private void testMaxmind(String path) throws URISyntaxException, IOException {
        AsnDatabase database = new AsnDatabase(resource.getFilePath(path));
        verifyAsnLookup(database, TELSTRA_IP_V4, "1.128.0.0/11", 1221L, "Telstra Pty Ltd");
        verifyAsnLookup(database, SUNRISE_IP_V6, "2001:1700:0:0:0:0:0:0/27", 6730L, "Sunrise Communications AG");

        testNoInfoAndNull(database);
    }

    @Test
    public void testNoMmdbInTar() {
        assertThatThrownBy(() -> new AsnDatabase(resource.getFilePath(IPINFO_NO_MMDB_TAR))).
                isInstanceOf(IllegalStateException.class).
                hasMessageContaining(IPINFO_NO_MMDB_TAR).
                hasMessageContaining("does not contain an mmdb file");
    }

    @Test
    public void testUnsupportedExtension() {
        assertThatThrownBy(() -> new AsnDatabase(resource.getFilePath(UNSUPPORTED_EXTENSION))).
                isInstanceOf(IllegalStateException.class).
                hasMessageContaining(UNSUPPORTED_EXTENSION).
                hasMessageContaining("unsupported extension");
    }

    @Test
    public void testThrowingDatabase() {
        String invalidDatabasePath = "geolite/invalid_maxmind_db.mmdb";
        assertThatThrownBy(() ->
                new AsnDatabase(resource.getFilePath(invalidDatabasePath))).
                isInstanceOf(IllegalStateException.class).
                hasMessageContaining("Could not read geocode database").
                hasMessageContaining(invalidDatabasePath);
    }

    private void verifyAsnLookup(AsnDatabase database, String ip, String network, long asnNumber, String organization) throws IOException {
        //noinspection rawtypes
        DatabaseRecord<Map> response = database.lookup(InetAddress.getByName(ip));

        assertEquals(network, database.getNetworkMask(response));
        assertEquals(asnNumber, database.getAsnNumber(response));
        assertEquals(organization, database.getAutonomousSystemOrganization(response));
    }

}
