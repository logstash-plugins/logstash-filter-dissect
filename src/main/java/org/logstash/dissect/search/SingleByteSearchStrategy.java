package org.logstash.dissect.search;

public class SingleByteSearchStrategy implements DelimiterSearchStrategy {
    @Override
    public int indexOf(byte[] needle, byte[] haystack, int offset) {
        for (int n = offset; n < haystack.length; n++) {
            if (haystack[n] == needle[0])
                return n;
        }
        return -1;
    }
}
