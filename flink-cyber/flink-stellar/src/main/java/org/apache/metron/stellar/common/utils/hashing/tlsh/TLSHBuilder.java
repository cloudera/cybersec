package org.apache.metron.stellar.common.utils.hashing.tlsh;


import java.nio.ByteBuffer;
import java.util.Arrays;

import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.T0;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.T11;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.T13;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.T2;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.T3;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.T5;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.T7;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHUtil.bucketMapping;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHUtil.fastBucketMapping;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHUtil.hexToBytes;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHUtil.lCapturing;
import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHUtil.swapNibble;

public class TLSHBuilder {
    private SlidingWindow slidingWindow;
    private final CHECKSUM_OPTION checksumOption;
    private final BUCKET_OPTION bucketOption;

    public TLSHBuilder() {
        this.slidingWindow = new SlidingWindow();
        this.checksumOption = CHECKSUM_OPTION.CHECKSUM_1;
        this.bucketOption = BUCKET_OPTION.BUCKET_128;
    }

    public TLSHBuilder(CHECKSUM_OPTION checksumOption, BUCKET_OPTION bucketOption) {
        this.slidingWindow = new SlidingWindow();
        this.checksumOption = checksumOption;
        this.bucketOption = bucketOption;
    }

    public TLSH create(final byte[] bytes) {
        return create(ByteBuffer.wrap(bytes));
    }

    public TLSH create(final ByteBuffer buffer) {
        int[] checksum = new int[checksumOption.getChecksumSize()];
        int[] preBuckets = new int[256];

        fill(buffer, checksum, preBuckets);

        final int[] sortedCopy = Arrays.copyOf(preBuckets, bucketOption.getBucketSize());
        Arrays.sort(sortedCopy);

        final int quartile = bucketOption.getBucketSize() / 4;
        final int p1 = quartile - 1;
        final int q1 = sortedCopy[p1];
        final int q2 = sortedCopy[p1 + quartile];
        final int q3 = sortedCopy[p1 + 2 * quartile];
        final int lValue = lCapturing(slidingWindow.getByteCount());
        final int q1ratio = (int) (q1 * 100.0f / q3) & 0x0F;
        final int q2ratio = (int) (q2 * 100.0f / q3) & 0x0F;

        final int[] compressedBucket = compress(preBuckets, q3, q2, q1);

        return new TLSH(checksum, compressedBucket, lValue, q1ratio, q2ratio);
    }

    public TLSH fromHex(String hashHex) {
        return fromHex(hexToBytes(hashHex));
    }

    public TLSH fromHex(byte[] hash) {
        final int bucketCount;
        final int checksumLength;
        switch (hash.length) {
            case 35:
                bucketCount = 128;
                checksumLength = 1;
                break;
            case 37:
                bucketCount = 128;
                checksumLength = 3;
                break;
            case 67:
                bucketCount = 256;
                checksumLength = 1;
                break;
            case 69:
                bucketCount = 256;
                checksumLength = 3;
                break;
            default:
                throw new IllegalArgumentException(
                        String.format("Illegal hash buffer length: %d, must be one of 35,37,67,69", hash.length));
        }
        final ByteBuffer buf = ByteBuffer.wrap(hash);
        final int[] checksum = new int[checksumLength];
        for (int i = 0; i < checksum.length; i++) {
            checksum[i] = swapNibble(buf.get() & 0xFF);
        }
        final int lValue = swapNibble(buf.get() & 0xFF);
        final int qRatio = buf.get() & 0xFF;
        final int q1Ratio = qRatio >> 4;
        final int q2Ratio = qRatio & 0x0F;
        final int[] codes = new int[bucketCount / 8 * 2];
        for (int i = 0; i < codes.length; i++) {
            codes[codes.length - 1 - i] = buf.get() & 0xFF;
        }
        return new TLSH(checksum, codes, lValue, q1Ratio, q2Ratio);
    }

    private void fill(ByteBuffer buffer, int[] checksum, int[] buckets) {
        if (buffer.remaining() < slidingWindow.getWindowSize()) {
            return;
        }
        slidingWindow.preFill(buffer);
        while (buffer.hasRemaining()) {
            slidingWindow.put(buffer.get());
            int[] window = slidingWindow.getWindow();
            processChecksum(checksum, window);
            processBuckets(buckets, window);
        }
    }

    private int[] compress(int[] buckets, long q3, long q2, long q1) {
        final int codeSize = bucketOption.getBucketSize() / 4;
        final int[] result = new int[codeSize];
        for (int i = 0; i < codeSize; i++) {
            int h = 0;
            for (int j = 0; j < 4; j++) {
                final long k = buckets[4 * i + j];
                if (q3 < k) {
                    h += 3 << j * 2;
                } else if (q2 < k) {
                    h += 2 << j * 2;
                } else if (q1 < k) {
                    h += 1 << j * 2;
                }
            }
            result[i] = h;
        }
        return result;
    }

    private static void processBuckets(int[] aBucket, int[] window) {
        aBucket[fastBucketMapping(T2, window[0], window[1], window[2])]++;
        aBucket[fastBucketMapping(T3, window[0], window[1], window[3])]++;
        aBucket[fastBucketMapping(T5, window[0], window[2], window[3])]++;

        aBucket[fastBucketMapping(T7, window[0], window[2], window[4])]++;
        aBucket[fastBucketMapping(T11, window[0], window[1], window[4])]++;
        aBucket[fastBucketMapping(T13, window[0], window[3], window[4])]++;
    }

    private void processChecksum(int[] checksum, int[] window) {
        if (checksumOption == CHECKSUM_OPTION.CHECKSUM_1) {
            checksum[0] = fastBucketMapping(T0, window[0], window[1], checksum[0]);
        } else {
            checksum[0] = fastBucketMapping(T0, window[0], window[1], checksum[0]);
            checksum[1] = bucketMapping(checksum[0], window[0], window[1], checksum[1]);
            checksum[2] = bucketMapping(checksum[1], window[0], window[1], checksum[2]);
        }
    }

    public void reset() {
        slidingWindow = new SlidingWindow();
    }


    public enum CHECKSUM_OPTION {
        CHECKSUM_1(1), CHECKSUM_3(3);

        private final int option;

        CHECKSUM_OPTION(int option) {
            this.option = option;
        }

        public int getChecksumSize() {
            return option;
        }

        public static CHECKSUM_OPTION fromVal(int val) {
            return Arrays.stream(values()).filter(op -> op.option == val).findFirst().orElse(CHECKSUM_1);
        }
    }

    public enum BUCKET_OPTION {
        BUCKET_128(128), BUCKET_256(256);

        private final int option;

        BUCKET_OPTION(int option) {
            this.option = option;
        }

        public int getBucketSize() {
            return this.option;
        }

        public static BUCKET_OPTION fromVal(int val) {
            return Arrays.stream(values()).filter(op -> op.option == val).findFirst().orElse(BUCKET_128);
        }
    }


}
