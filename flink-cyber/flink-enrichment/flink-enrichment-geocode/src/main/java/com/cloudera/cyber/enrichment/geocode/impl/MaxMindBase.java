package com.cloudera.cyber.enrichment.geocode.impl;

import avro.shaded.com.google.common.base.Preconditions;
import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import com.maxmind.geoip2.DatabaseProvider;
import lombok.NonNull;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

public abstract class MaxMindBase {
    public static final String FIELD_VALUE_IS_NOT_A_STRING = "'%s' is not a String.";
    public static final String FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS = "'%s' is not a valid IP address.";
    public static final String MAXMIND_FAILED_MESSAGE = "Maxmind lookup failed '%s'";

    /**
     * Parsed and cached Maxmind database.
     */
    @NonNull
    protected final DatabaseProvider database;

    public MaxMindBase(DatabaseProvider database) {
        Preconditions.checkNotNull(database);
        this.database = database;
    }
    protected static String convertEmptyToNull(String str) {
        return StringUtils.isBlank(str) ? null : str;
    }

    protected InetAddress convertToIpAddress(Enrichment enrichment, Object ipValueObject, List<DataQualityMessage> qualityMessages) {
        InetAddress inetAddress = null;
        if (ipValueObject instanceof String) {
            String ipValue = (String) ipValueObject;
            if (InetAddressValidator.getInstance().isValid(ipValue)) {
                try {
                    inetAddress = InetAddress.getByName(ipValue);
                    if (inetAddress.isSiteLocalAddress() ||
                            inetAddress.isAnyLocalAddress()  ||
                            inetAddress.isLinkLocalAddress() ||
                            inetAddress.isLoopbackAddress() ||
                            inetAddress.isMulticastAddress()) {
                        // internal network addresses won't have geo info so stop here
                        inetAddress = null;
                    }
                } catch (UnknownHostException e) {
                    // this should not happen - checks for valid IP prior to call
                    enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.INFO, String.format(MAXMIND_FAILED_MESSAGE, e.getMessage()));
                }
            } else {
                enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.INFO, String.format(FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS, ipValue));
            }
        } else {
            enrichment.addQualityMessage(qualityMessages, DataQualityMessageLevel.INFO, String.format(FIELD_VALUE_IS_NOT_A_STRING, ipValueObject.toString()));
        }

        return inetAddress;
    }

}
