/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.apache.metron.stellar.common.utils.hashing.tlsh;

import java.nio.ByteBuffer;

import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHUtil.swapNibble;

/**
 * The abstraction around interacting with TLSH.
 */
public class TLSH {

    /**
     * The checksum bytes.
     */
    private final int[] checksum;
    /**
     * The buckets bytes.
     */
    private final int[] codes;
    /**
     * The encoded length value.
     */
    private final int lValue;
    /**
     * The q1 ratio.
     */
    private final int q1Ratio;
    /**
     * The q2 ratio.
     */
    private final int q2Ratio;


    public TLSH(int[] checksum, int[] codes, int lValue, int q1, int q2) {
        this.checksum = checksum;
        this.codes = codes;
        this.lValue = lValue;
        this.q1Ratio = q1;
        this.q2Ratio = q2;
    }


    public String getHash() {
        return TLSHUtil.bytesToHex(getHexBytes());
    }

    public int[] getChecksum() {
        return checksum;
    }

    public int[] getCodes() {
        return codes;
    }

    public int getlValue() {
        return lValue;
    }

    public int getQ1Ratio() {
        return q1Ratio;
    }

    public int getQ2Ratio() {
        return q2Ratio;
    }

    public byte[] getHexBytes() {
        final ByteBuffer buf = ByteBuffer.allocate(checksum.length + 2 + codes.length);
        for (final int c : checksum) {
            buf.put((byte) swapNibble(c));
        }
        buf.put((byte) swapNibble(lValue));
        buf.put((byte) (q1Ratio << 4 | q2Ratio));
        for (int i = codes.length - 1; i >= 0; i--) {
            buf.put((byte) codes[i]);
        }
        buf.flip();
        if (buf.hasArray() && 0 == buf.arrayOffset()) {
            return buf.array();
        } else {
            final byte[] hash = new byte[buf.remaining()];
            buf.get(hash);
            return hash;
        }
    }
}
