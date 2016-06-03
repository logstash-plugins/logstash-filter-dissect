package org.logstash.dissect;

class DelimTwo implements IDelim {
    private final byte b1;
    private final byte b2;
    private final String _string;
    private final int length = 2;

    public DelimTwo(String delim) {
        _string = delim;
        byte[] b = delim.getBytes();
        b1 = b[0];
        b2 = b[1];
    }

    public int indexOf(byte[] data, int offset) {
        return BruteForce.indexOf(data, offset, b1, b2);
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
