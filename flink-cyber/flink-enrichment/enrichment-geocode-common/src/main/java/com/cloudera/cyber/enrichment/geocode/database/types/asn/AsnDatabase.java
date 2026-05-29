package com.cloudera.cyber.enrichment.geocode.database.types.asn;

import com.cloudera.cyber.enrichment.geocode.database.MaxmindDatabase;
import com.maxmind.db.DatabaseRecord;
import com.maxmind.db.Reader;

import java.io.IOException;
import java.net.InetAddress;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class AsnDatabase extends MaxmindDatabase {
    private final AsnDatabaseResponseDecoder responseDecoder;

    public AsnDatabase(Reader database) {
        super(database);
        this.responseDecoder = getDecoder();
    }

    public AsnDatabase(String geocodeDatabasePath) {
        super(geocodeDatabasePath);
        this.responseDecoder = getDecoder();
    }

    private AsnDatabaseResponseDecoder getDecoder() {
        return getDatabaseVendor().getAsnDecoder();
    }

    public DatabaseRecord<Map> lookup(InetAddress ipAddress) throws IOException {
        if (ipAddress != null) {
            DatabaseRecord<Map> response = database.getRecord(ipAddress, Map.class);
            if (response.data() != null) {
                return response;
            }
        }
        return null;
    }

    public Object getAsnNumber(DatabaseRecord<Map> response) {
        if (response != null) {
            //noinspection unchecked
            return this.responseDecoder.getAsnNumber(response.data());
        } else {
            return null;
        }
    }

    public Object getAutonomousSystemOrganization(DatabaseRecord<Map> response) {
        if (response != null) {
            //noinspection unchecked
            return this.responseDecoder.getAutonomousSystemOrganization(response.data());
        } else {
            return null;
        }
    }

    public Object getNetworkMask(DatabaseRecord<Map> response) {
        if (response != null) {
            return response.network().toString();
        } else {
            return null;
        }
    }
}
