package org.apache.metron.stellar.common.utils.hashing.tlsh;


import java.util.function.IntUnaryOperator;
import java.util.stream.IntStream;

public class SlidingWindow {
    public static final int DEFAULT_SIZE = 5;
    private final byte[] window;
    private int byteCount = 0;

    SlidingWindow() {
        this.window = new byte[DEFAULT_SIZE];
    }

    public void put(final byte value) {
        int cursor = byteCount % window.length;
        window[cursor] = value;
        byteCount++;
    }

    public int[] getWindow() {
        final int startPosition = (byteCount - 1) % window.length;
        final IntUnaryOperator reverseIterate = i -> i == 0 ? window.length - 1 : i - 1;
        final IntUnaryOperator mapper = i -> window[i] & 0xFF;
        return IntStream.iterate(startPosition, reverseIterate)
                        .limit(window.length)
                        .map(mapper)
                        .toArray();
    }

    public int getByteCount() {
        return byteCount;
    }

    public int getWindowSize() {
        return window.length;
    }
}
