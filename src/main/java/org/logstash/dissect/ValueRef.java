package org.logstash.dissect;

import java.nio.charset.StandardCharsets;

final class ValueRef {
    private final String fieldName;
    private int position;
    private int length;

    ValueRef(final String fieldName) {
        this.fieldName = fieldName;
        this.position = 0;
        this.length = 0;
    }

    public String getFieldName() {
        return fieldName;
    }

    public void update(final int position, final int length) {
        this.position = position;
        this.length = length;
    }

    public void clear() {
        this.position = 0;
        this.length = 0;
    }

    String extract(final byte[] source) {
        return new String(source, position, length, StandardCharsets.UTF_8);
    }

    @Override
    public String toString() {
        return "ValueRef{fieldName=" + fieldName +
                ", position=" + position +
                ", length=" + length + '}';
    }
}
