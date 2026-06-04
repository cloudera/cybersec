/*
 * Copyright 2020 - 2022 Cloudera. All Rights Reserved.
 *
 * This file is licensed under the Apache License Version 2.0 (the "License"). You may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0.
 *
 * This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND,
 * either express or implied. Refer to the License for the specific permissions and
 * limitations governing your use of the file.
 */

package com.cloudera.cyber.enrichment.geocode.database;

import com.cloudera.cyber.DataQualityMessage;
import com.cloudera.cyber.DataQualityMessageLevel;
import com.cloudera.cyber.enrichment.Enrichment;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.validator.routines.InetAddressValidator;

import java.io.IOException;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;

@Slf4j
public abstract class IpEnrichment {
    public static final String FIELD_VALUE_IS_NOT_A_STRING = "'%s' is not a String.";
    public static final String FIELD_VALUE_IS_NOT_A_VALID_IP_ADDRESS = "'%s' is not a valid IP address.";
    public static final String MAXMIND_FAILED_MESSAGE = "Maxmind lookup failed '%s'";

    public abstract void close() throws IOException;

    protected InetAddress convertToIpAddress(Enrichment enrichment, Object ipValueObject, List<DataQualityMessage> qualityMessages) {
        InetAddress inetAddress = null;
        if (ipValueObject instanceof String ipValue) {
            if (InetAddressValidator.getInstance().isValid(ipValue)) {
                try {
                    inetAddress = InetAddress.getByName(ipValue);
                    if (inetAddress.isSiteLocalAddress() ||
                            inetAddress.isAnyLocalAddress() ||
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
