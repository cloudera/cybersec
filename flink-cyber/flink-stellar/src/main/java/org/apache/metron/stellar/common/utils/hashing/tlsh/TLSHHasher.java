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

import org.apache.commons.codec.DecoderException;
import org.apache.commons.codec.binary.Hex;
import org.apache.metron.stellar.common.utils.ConversionUtils;
import org.apache.metron.stellar.common.utils.SerDeUtils;
import org.apache.metron.stellar.common.utils.hashing.EnumConfigurable;
import org.apache.metron.stellar.common.utils.hashing.Hasher;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.BitSet;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;

public class TLSHHasher implements Hasher {
    public static final String TLSH_KEY = "tlsh";
    public static final String TLSH_BIN_KEY = "tlsh_bin";

    public enum Config implements EnumConfigurable {
        BUCKET_SIZE("bucketSize"),
        CHECKSUM("checksumBytes"),
        HASHES("hashes"),
        FORCE("force");
        final public String key;

        Config(String key) {
            this.key = key;
        }

        @Override
        public String getKey() {
            return key;
        }
    }

    Integer bucketOption = 128;
    Integer checksumOption = 1;
    Boolean force = true;
    List<Integer> hashes = new ArrayList<>();

    /**
     * Returns an encoded string representation of the hash value of the input. It is expected that
     * this implementation does throw exceptions when the input is null.
     *
     * @param o The value to hash.
     * @return A hash of {@code toHash} that has been encoded.
     *
     */
    @Override
    public Object getHash(Object o) {
        TLSHBuilder builder = new TLSHBuilder(TLSHBuilder.CHECKSUM_OPTION.fromVal(checksumOption), TLSHBuilder.BUCKET_OPTION.fromVal(bucketOption));
        byte[] data;
        if (o instanceof String) {
            data = ((String) o).getBytes(StandardCharsets.UTF_8);
        } else if (o instanceof byte[]) {
            data = (byte[]) o;
        } else {
            data = SerDeUtils.toBytes(o);
        }
        try {
            TLSH tlsh = builder.getTLSH(data);
            builder.clean();
            String hash = tlsh.getHash();
            if (hashes != null && !hashes.isEmpty()) {
                Map<String, Object> ret = new HashMap<>();
                ret.put(TLSH_KEY, hash);
                ret.putAll(bin(hash));
                return ret;
            } else {
                return hash;
            }
        } catch (Exception e) {
            return null;
        }
    }

    public Map<String, String> bin(String hash) throws DecoderException {
        Random r = new Random(0);
        byte[] h = Hex.decodeHex(hash.substring(2 * checksumOption).toCharArray());
        BitSet vector = BitSet.valueOf(h);
        int n = vector.length();
        Map<String, String> ret = new HashMap<>();
        boolean singleHash = hashes.size() == 1;
        for (int numHashes : hashes) {
            BitSet projection = new BitSet();
            for (int i = 0; i < numHashes; ++i) {
                int index = r.nextInt(n);
                projection.set(i, vector.get(index));
            }
            String outputHash = numHashes + Hex.encodeHexString(projection.toByteArray());
            if (singleHash) {
                ret.put(TLSH_BIN_KEY, outputHash);
            } else {
                ret.put(TLSH_BIN_KEY + "_" + numHashes, outputHash);
            }
        }
        return ret;
    }

    @Override
    public void configure(Optional<Map<String, Object>> config) {
        if (config.isPresent() && !config.get().isEmpty()) {
            bucketOption = Config.BUCKET_SIZE.get(config.get()
                    , o -> {
                        Integer bucketSize = ConversionUtils.convert(o, Integer.class);
                        return bucketSize.equals(256) ? 256 : 128;
                    }
            ).orElse(bucketOption);

            checksumOption = Config.CHECKSUM.get(config.get()
                    , o -> {
                        Integer checksumBytes = ConversionUtils.convert(o, Integer.class);
                        return checksumBytes.equals(3) ? 3 : 1;
                    }
            ).orElse(checksumOption);

            force = Config.FORCE.get(config.get()
                    , o -> ConversionUtils.convert(o, Boolean.class)
            ).orElse(force);

            hashes = Config.HASHES.get(config.get()
                    , o -> {
                        List<Integer> ret = new ArrayList<>();
                        if (o instanceof List) {
                            List<?> vals = (List<?>) o;
                            for (Object oVal : vals) {
                                ret.add(ConversionUtils.convert(oVal, Integer.class));
                            }
                        } else {
                            ret.add(ConversionUtils.convert(o, Integer.class));
                        }
                        return ret;
                    }
            ).orElse(hashes);
        }
    }

    public static Set<String> supportedHashes() {
        return new HashSet<String>() {{
            add("TLSH");
        }};
    }

}
