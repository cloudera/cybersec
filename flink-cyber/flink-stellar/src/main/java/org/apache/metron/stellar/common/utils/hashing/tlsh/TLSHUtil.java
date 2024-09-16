package org.apache.metron.stellar.common.utils.hashing.tlsh;


import java.nio.charset.StandardCharsets;
import java.util.Arrays;

import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.HEX_ARRAY;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.LEN_ADJ_2;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.LEN_ADJ_3;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.LEN_STEP_1;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.LEN_STEP_2;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.LOG_1_1;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.LOG_1_3;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.LOG_1_5;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.PEARSON_TABLE;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.TOPVAL;

public final class TLSHUtil {


    private TLSHUtil() {
    }

    public static int hash(final int salt, final int i, final int j, final int k) {
        int res = 0;
        res = PEARSON_TABLE[res ^ salt];
        res = PEARSON_TABLE[res ^ i];
        res = PEARSON_TABLE[res ^ j];
        res = PEARSON_TABLE[res ^ k];
        return res;
    }

    public static int bucketMapping(final int salt, final int i, final int j, final int k) {
        return PEARSON_TABLE[PEARSON_TABLE[PEARSON_TABLE[PEARSON_TABLE[salt] ^ i] ^ j] ^ k];
    }

    public static int fastBucketMapping(final int mod_salt, final int i, final int j, final int k) {
        return PEARSON_TABLE[PEARSON_TABLE[PEARSON_TABLE[mod_salt ^ i] ^ j] ^ k];
    }

    /**
     * Capture the log(length) in a single byte value.
     *
     * @param len the length
     * @return the byte value
     */
    public static int lCapturing(final int len) {
        final int x = Arrays.binarySearch(TOPVAL, len);
        return x >= 0 ? x : -x - 1;
    }

    /**
     * Capture the log(length) in a single byte value.
     *
     * <p>
     * Math.log based implementation.
     *
     * @param len the length
     * @return the byte value
     */
    public static int lCapturingLog(final int len) {
        if (len <= 0) {
            return 0;
        }
        double d = (float) Math.log((float) len);
        if (len <= LEN_STEP_1) {
            d = d / LOG_1_5;
        } else if (len <= LEN_STEP_2) {
            d = d / LOG_1_3 - LEN_ADJ_2;
        } else {
            d = d / LOG_1_1 - LEN_ADJ_3;
        }
        return Math.min((int) Math.floor(d), 255);
    }

    public static String bytesToHex(byte[] bytes) {
        byte[] hexChars = new byte[bytes.length * 2];
        for (int i = 0; i < bytes.length; i++) {
            int unSignByte = bytes[i] & 0xFF;
            hexChars[i * 2] = HEX_ARRAY[unSignByte >>> 4];
            hexChars[i * 2 + 1] = HEX_ARRAY[unSignByte & 0x0F];
        }
        return new String(hexChars, StandardCharsets.UTF_8);
    }

    public static byte[] hexToBytes(final CharSequence hex) {
        final int len = hex.length();
        final byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(hex.charAt(i), 16) << 4) + Character.digit(hex.charAt(i + 1), 16));
        }
        return data;
    }

    public static int swapNibble(final int x) {
        return (x & 0x0F) << 4 | (x & 0xF0) >> 4;
    }


}
