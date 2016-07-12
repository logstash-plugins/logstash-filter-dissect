package org.logstash.dissect;

import java.util.Arrays;

public class ValueRef {
    private int position = 0;
    private int length = 0;

    public void set(int position, int length) {
        this.position = position;
        this.length = length;
    }

    public void setPosition(int position) {
        this.position = position;
    }

    public void setLength(int length) {
        this.length = length;
    }

    public String string(byte[] source) {
        return new String(source, position, length);
    }

    public byte[] bytes(byte[] source) {
        return Arrays.copyOfRange(source, position, length);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ValueRef{");
        sb.append("position=").append(position);
        sb.append(", length=").append(length);
        sb.append('}');
        return sb.toString();
    }
}
