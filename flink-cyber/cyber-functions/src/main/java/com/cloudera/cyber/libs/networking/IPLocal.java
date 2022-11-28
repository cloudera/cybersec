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
