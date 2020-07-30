package com.cloudera.cyber.libs.hostnames;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.AbstractStringScalarFunction;
import com.google.common.net.InternetDomainName;
import org.apache.commons.lang3.StringUtils;

import java.util.function.Function;

/**
 * Extracts parts of domain names
 *
 * Uses https://guava.dev/releases/24.0-jre/api/docs/com/google/common/net/InternetDomainName.html
 * which relies on https://publicsuffix.org/list/ for TLDs
 */
@CyberFunction("extract_hostname")
public class ExtractHostname extends AbstractStringScalarFunction {

    public static final String REVERSE_IP_SUFFIX = "in-addr.arpa";

    public enum HostnameFeature {
        TLD(dns -> dns.publicSuffix()),
        NO_TLD(dns -> removeTld(dns)),
        NO_SUBS(dns ->dns.topPrivateDomain()),
        NO_SUBS_NO_TLD(dns -> removeTld(dns.topPrivateDomain()));

        protected Function<InternetDomainName, InternetDomainName> process;

        private static InternetDomainName removeTld(InternetDomainName dns) {
            return InternetDomainName.from(StringUtils.removeEnd(dns.toString(), dns.publicSuffix().toString()));
        }

        private HostnameFeature(Function<InternetDomainName, InternetDomainName> process) {
            this.process = process;
        }
    }

    public String eval(String hostname, HostnameFeature feature) {
        if (hostname.endsWith(REVERSE_IP_SUFFIX) || hostname.endsWith(REVERSE_IP_SUFFIX + ".")) {
            return reverseResult(hostname, feature);
        }
        return feature.process.apply(InternetDomainName.from(hostname)).toString();
    }

    public static String reverseResult(String hostname, HostnameFeature feature) {
        return feature.equals(HostnameFeature.TLD) ? REVERSE_IP_SUFFIX :
                StringUtils.reverseDelimited(StringUtils.removeEnd(hostname, REVERSE_IP_SUFFIX),'.');
    }

}
