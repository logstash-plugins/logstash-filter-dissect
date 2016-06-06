package org.logstash.dissect;

class DelimMany implements IDelim {
    private final byte[] bytes;
    private final String _string;
    private final int length;

    public DelimMany(String delim) {
        _string = delim;
        bytes = delim.getBytes();
        length = bytes.length;
    }

    public int indexOf(byte[] data, int offset) {
        return IndexOfBytes.indexOf(data, offset, bytes);
    }

    public int size() {
        return length;
    }

    public String string() {
        return _string;
    }

    @Override
    public String toString() {
        return "'" + _string + "'";
    }
}
