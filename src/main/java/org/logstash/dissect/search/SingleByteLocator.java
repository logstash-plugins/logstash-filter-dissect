package org.logstash.dissect.search;

public class SingleByteLocator implements DelimiterLocator {
    public static SingleByteLocator INSTANCE = new SingleByteLocator();

    private SingleByteLocator() {
    }

    @Override
    public int indexOf(byte[] needle, byte[] haystack, int offset) {
        for (int n = offset; n < haystack.length; n++) {
            if (haystack[n] == needle[0])
                return n;
        }
        return -1;
    }
}
