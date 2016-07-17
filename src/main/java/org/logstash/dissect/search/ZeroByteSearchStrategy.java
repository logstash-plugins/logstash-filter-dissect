package org.logstash.dissect.search;

public class ZeroByteSearchStrategy implements DelimiterSearchStrategy {
    @Override
    public int indexOf(byte[] needle, byte[] haystack, int offset) {
        return -1;
    }
}
