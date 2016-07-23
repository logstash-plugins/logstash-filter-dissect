package org.logstash.dissect;

import org.logstash.dissect.search.DelimiterLocator;
import org.logstash.dissect.search.DoubleByteLocator;
import org.logstash.dissect.search.MultiByteLocator;
import org.logstash.dissect.search.SingleByteLocator;
import org.logstash.dissect.search.ZeroByteLocator;

public final class Delimiter {
    private final byte[] needle;
    private final String delimiter;
    private final DelimiterLocator strategy;

    private Delimiter(String delimiter, byte[] bytes, DelimiterLocator strategy) {
        this.delimiter = delimiter;
        this.needle = bytes;
        this.strategy = strategy;
    }

    public static Delimiter create(String delim) {
        byte[] bytes = delim.getBytes();
        switch (bytes.length) {
            case 0:
                return new Delimiter(delim, bytes, ZeroByteLocator.INSTANCE);
            case 1:
                return new Delimiter(delim, bytes, SingleByteLocator.INSTANCE);
            case 2:
                return new Delimiter(delim, bytes, DoubleByteLocator.INSTANCE);
            default:
                return new Delimiter(delim, bytes, MultiByteLocator.INSTANCE);
        }
    }

    public int indexOf(byte[] haystack, int offset) {
        return strategy.indexOf(this.needle, haystack, offset);
    }

    public int size() {
        return this.needle.length;
    }

    public String getDelimiter() {
        return this.delimiter;
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("Delimiter{");
        sb.append("delimiter='").append(delimiter);
        sb.append("', size=").append(size());
        sb.append('}');
        return sb.toString();
    }
}
