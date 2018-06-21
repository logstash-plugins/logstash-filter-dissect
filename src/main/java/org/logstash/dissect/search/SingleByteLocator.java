package org.logstash.dissect.search;

public class SingleByteLocator implements DelimiterLocator {
    public static final SingleByteLocator INSTANCE = new SingleByteLocator();

    private SingleByteLocator() {
    }

    @Override
    public final int indexOf(final byte[] needle, final byte[] haystack, final int offset) {
        for (int n = offset; n < haystack.length; n++) {
            if (haystack[n] == needle[0]) {
                return n;
            }
        }
        return -1;
    }
}
