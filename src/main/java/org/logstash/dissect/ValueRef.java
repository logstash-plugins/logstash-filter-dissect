package org.logstash.dissect;

public class ValueRef {
    private int _position = 0;
    private int _length = 0;

    public void set(int position, int length) {
        _position = position;_length = length;
    }

    public void set_position(int position) {
        _position = position;
    }

    public void set_length(int length) {
        _length = length;
    }

    public String value(byte[] source) {
        return new String(source, _position, _length);
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("ValueRef{");
        sb.append("_position=").append(_position);
        sb.append(", _length=").append(_length);
        sb.append('}');
        return sb.toString();
    }
}
