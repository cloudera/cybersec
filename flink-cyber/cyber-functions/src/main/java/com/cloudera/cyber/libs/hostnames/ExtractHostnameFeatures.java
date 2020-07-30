package com.cloudera.cyber.libs.hostnames;

import com.cloudera.cyber.libs.AbstractMapScalarFunction;
import com.google.common.net.InternetDomainName;

import java.util.HashMap;
import java.util.Map;

import static com.cloudera.cyber.libs.hostnames.ExtractHostname.REVERSE_IP_SUFFIX;
import static com.cloudera.cyber.libs.hostnames.ExtractHostname.reverseResult;

public class ExtractHostnameFeatures extends AbstractMapScalarFunction {

    public Map<String,String> eval(String hostname) {
        HashMap<String, String> results = new HashMap<String, String>(ExtractHostname.HostnameFeature.values().length);
        if (hostname.endsWith(REVERSE_IP_SUFFIX) || hostname.endsWith(REVERSE_IP_SUFFIX + ".")) {
            // optimisation, all the features except TLD are the same, the duplication is for consistency for users
            String ip = reverseResult(hostname, ExtractHostname.HostnameFeature.NO_TLD);
            for (ExtractHostname.HostnameFeature feature : ExtractHostname.HostnameFeature.values()) {
                results.put(feature.name(), ip);
            }
            results.put("TLD", REVERSE_IP_SUFFIX);
        } else {
            for (ExtractHostname.HostnameFeature feature : ExtractHostname.HostnameFeature.values()) {
                results.put(feature.name(), feature.process.apply(InternetDomainName.from(hostname)).toString());
            }
        }
        return results;
    }
}
