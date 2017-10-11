package org.logstash.dissect.search;

public class ZeroByteLocator implements DelimiterLocator {
    public static final ZeroByteLocator INSTANCE = new ZeroByteLocator();

    private ZeroByteLocator() {
    }

    @Override
    public int indexOf(final byte[] needle, final byte[] haystack, final int offset) {
        return -1;
    }
}
