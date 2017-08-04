package org.logstash.dissect.search;

public class DoubleByteLocator implements DelimiterLocator {
    public static final DoubleByteLocator INSTANCE = new DoubleByteLocator();

    private DoubleByteLocator() {
    }

    @Override
    public int indexOf(final byte[] needle, final byte[] haystack, final int offset) {
        for (int n = offset; n < haystack.length - 1; n++) {
            if (haystack[n] == needle[0] && haystack[n + 1] == needle[1]) {
                return n;
            }
        }
        return -1;
    }
}
