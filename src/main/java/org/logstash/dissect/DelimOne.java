package org.logstash.dissect;

class DelimOne implements IDelim {
    private final byte b;
    private final String _string;
    private final int length = 1;

    public DelimOne(String delim) {
        _string = delim;
        b = delim.getBytes()[0];
    }

    public int indexOf(byte[] data, int offset) {
        return BruteForce.indexOf(data, offset, b);
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
