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

package com.cloudera.cyber.libs.hostnames;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.AbstractStringScalarFunction;
import com.google.common.net.InternetDomainName;
import java.util.function.Function;
import org.apache.commons.lang3.StringUtils;

/**
 * Extracts parts of domain names
 *
 * <p>
 * Uses https://guava.dev/releases/24.0-jre/api/docs/com/google/common/net/InternetDomainName.html
 * which relies on https://publicsuffix.org/list/ for TLDs
 */
@CyberFunction("extract_hostname")
public class ExtractHostname extends AbstractStringScalarFunction {

    public static final String REVERSE_IP_SUFFIX = "in-addr.arpa";

    public static String reverseResult(String hostname, HostnameFeature feature) {
        return feature.equals(HostnameFeature.TLD) ? REVERSE_IP_SUFFIX :
              StringUtils.reverseDelimited(StringUtils.removeEnd(hostname, REVERSE_IP_SUFFIX), '.');
    }

    public String eval(String hostname, HostnameFeature feature) {
        if (hostname.endsWith(REVERSE_IP_SUFFIX) || hostname.endsWith(REVERSE_IP_SUFFIX + ".")) {
            return reverseResult(hostname, feature);
        }
        return feature.process.apply(InternetDomainName.from(hostname)).toString();
    }

    public enum HostnameFeature {
        TLD(dns -> dns.publicSuffix()),
        NO_TLD(dns -> removeTld(dns)),
        NO_SUBS(dns -> dns.topPrivateDomain()),
        NO_SUBS_NO_TLD(dns -> removeTld(dns.topPrivateDomain()));

        protected Function<InternetDomainName, InternetDomainName> process;

        private HostnameFeature(Function<InternetDomainName, InternetDomainName> process) {
            this.process = process;
        }

        private static InternetDomainName removeTld(InternetDomainName dns) {
            return InternetDomainName.from(StringUtils.removeEnd(dns.toString(), dns.publicSuffix().toString()));
        }
    }

}
