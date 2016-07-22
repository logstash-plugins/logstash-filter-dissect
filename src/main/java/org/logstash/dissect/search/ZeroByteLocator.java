package org.logstash.dissect.search;

public class ZeroByteLocator implements DelimiterLocator {
    public static ZeroByteLocator INSTANCE = new ZeroByteLocator();

    private ZeroByteLocator() {
    }

    @Override
    public int indexOf(byte[] needle, byte[] haystack, int offset) {
        return -1;
    }
}
