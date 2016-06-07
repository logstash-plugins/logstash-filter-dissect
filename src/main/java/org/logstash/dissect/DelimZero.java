package org.logstash.dissect;

class DelimZero implements IDelim {
    private final byte b;
    private final String _string;
    private final int length = 1;

    public DelimZero() {
        _string = "";
        b = 0;
    }

    @Override
    public int indexOf(byte[] text, int textStart) {
        return -1;
    }

    @Override
    public int size() {
        return length;
    }

    @Override
    public String string() {
        return _string;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DelimZero{");
        sb.append("string='").append(_string);
        sb.append("', length=").append(length);
        sb.append('}');
        return sb.toString();
    }
}
