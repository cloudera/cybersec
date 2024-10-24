/**
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 * 
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */


package org.apache.metron.parsers.ise;

import com.esotericsoftware.minlog.Log;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.apache.metron.parsers.BasicParser;
import org.json.simple.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class BasicIseParser extends BasicParser {

    private static final Logger _LOG = LoggerFactory
          .getLogger(BasicIseParser.class);
    static final ISEParser _parser = new ISEParser("header=");

    @Override
    public void configure(Map<String, Object> parserConfig) {
        setReadCharset(parserConfig);
    }

    @Override
    public void init() {

    }

    @SuppressWarnings({"unchecked", "checkstyle:VariableDeclarationUsageDistance"})
    @Override
    public List<JSONObject> parse(byte[] msg) {

        String rawMessage = "";
        List<JSONObject> messages = new ArrayList<>();
        try {

            rawMessage = new String(msg, getReadCharset());
            _LOG.debug("Received message: {}", rawMessage);

            /*
             * Reinitialize Parser. It has the effect of calling the constructor again.
             */
            _parser.ReInit(new StringReader("header=" + rawMessage.trim()));

            JSONObject payload = _parser.parseObject();

            String ipSrcAddr = (String) payload.get("Device IP Address");
            String ipSrcPort = (String) payload.get("Device Port");
            String ipDstAddr = (String) payload.get("DestinationIPAddress");
            String ipDstPort = (String) payload.get("DestinationPort");

            /*
             * Standard Fields for Metron.
             */

            if (ipSrcAddr != null) {
                payload.put("ip_src_addr", ipSrcAddr);
            }
            if (ipSrcPort != null) {
                payload.put("ip_src_port", ipSrcPort);
            }
            if (ipDstAddr != null) {
                payload.put("ip_dst_addr", ipDstAddr);
            }
            if (ipDstPort != null) {
                payload.put("ip_dst_port", ipDstPort);
            }
            messages.add(payload);
            return messages;

        } catch (Exception e) {
            Log.error(e.toString());
            e.printStackTrace();
        }
        return null;
    }

    @Override
    public boolean validate(JSONObject message) {
        return true;
    }


}
