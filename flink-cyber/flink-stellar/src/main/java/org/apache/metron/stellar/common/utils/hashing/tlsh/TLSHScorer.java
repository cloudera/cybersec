package org.apache.metron.stellar.common.utils.hashing.tlsh;

import static org.apache.metron.stellar.common.utils.hashing.tlsh.TLSHConstants.DIFF_SCALE;

import java.util.Arrays;

public class TLSHScorer {

    private final BitPairsTable diffTable;

    public TLSHScorer() {
        this.diffTable = new BitPairsTable();
    }

    public int score(TLSH tlsh1, TLSH tlsh2, boolean lenDiff) {
        int score = 0;

        score += scoreChecksum(tlsh1.getChecksum(), tlsh2.getChecksum());
        if (lenDiff) {
            score += scoreLValue(tlsh1.getlValue(), tlsh2.getlValue());
        }
        score += scoreQ(tlsh1.getQ1Ratio(), tlsh2.getQ1Ratio());
        score += scoreQ(tlsh1.getQ2Ratio(), tlsh2.getQ2Ratio());
        score += scoreBuckets(tlsh1.getCodes(), tlsh2.getCodes());

        return score;
    }

    private int scoreBuckets(final int[] buckets1, final int[] buckets2) {
        if (buckets1.length != buckets2.length) {
            throw new IllegalArgumentException(
                  String.format("Number of body bytes differ %d != %d", buckets1.length, buckets2.length));
        }

        int diff = 0;
        for (int i = 0; i < buckets1.length; i++) {
            diff += this.diffTable.getValue(buckets1[i], buckets2[i]);
        }
        return diff;
    }

    private int scoreChecksum(final int[] checksumA, final int[] checksumB) {
        if (checksumA.length != checksumB.length) {
            throw new IllegalArgumentException(
                  String.format("Number of checksum bytes differ %d != %d", checksumA.length, checksumB.length));
        }
        return Arrays.equals(checksumA, checksumB) ? 0 : 1;
    }

    private int scoreQ(final int q2, final int q3) {
        final int q1diff = modDiff(q2, q3, 16);

        return q1diff <= 1 ? q1diff : (q1diff - 1) * DIFF_SCALE;
    }

    @SuppressWarnings("checkstyle:ParameterName")
    private int scoreLValue(final int lValue2, final int lValue3) {
        final int ldiff = modDiff(lValue2, lValue3, 256);
        switch (ldiff) {
            case 0:
                return 0;
            case 1:
                return 1;
            default:
                return DIFF_SCALE * ldiff;
        }
    }

    private int modDiff(final int initialPosition, final int finalPosition, final int circularQueueSize) {
        int internalDistance = Math.abs(finalPosition - initialPosition);
        int externalDistance = circularQueueSize - internalDistance;

        return Math.min(internalDistance, externalDistance);
    }
}
