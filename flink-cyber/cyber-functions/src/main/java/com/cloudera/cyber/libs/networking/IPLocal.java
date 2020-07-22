package com.cloudera.cyber.libs.networking;

import com.cloudera.cyber.CyberFunction;
import com.cloudera.cyber.libs.AbstractBooleanScalarFunction;

import java.net.InetAddress;
import java.net.UnknownHostException;

@CyberFunction("ip_local")
public class IPLocal extends AbstractBooleanScalarFunction {
    public Boolean eval(String ip) {
        try {
            return InetAddress.getByName(ip).isSiteLocalAddress();
        } catch (UnknownHostException e) {
            return false;
        }
    }
}
