package org.logstash.dissect;

import org.logstash.dissect.search.DelimiterSearchStrategy;

public final class Delimiter {
    private final byte[] needle;
    private final String delimiter;
    private final int size;
    private DelimiterSearchStrategy strategy;

    public Delimiter(String delim) {
        delimiter = delim;
        needle = delim.getBytes();
        size = needle.length;
    }

    public Delimiter addStrategy(DelimiterSearchStrategy strategy) {
        this.strategy = strategy;
        return this;
    }

    public int indexOf(byte[] haystack, int offset) {
        return strategy.indexOf(this.needle, haystack, offset);
    }

    public int size() {
        return this.size;
    }

    public String delimiterString() {
        return this.delimiter;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Delimiter{");
        sb.append("delimiterString='").append(delimiter);
        sb.append("', size=").append(size);
        sb.append('}');
        return sb.toString();
    }
}
