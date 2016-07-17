package org.logstash.dissect.search;

public interface DelimiterSearchStrategy {
    int indexOf(byte[] needle, byte[] haystack, int offset);
}
