package org.logstash.dissect;

class DelimMany implements IDelim {
    private final byte[] bytes;
    private final String _string;
    private final int length;
    private final Object helper;

    public DelimMany(String delim) {
        _string = delim;
        bytes = delim.getBytes();
        length = bytes.length;
        helper = ByteSearch.getHelper(bytes);
    }

    public int indexOf(byte[] data, int offset) {
        return ByteSearch.indexOf(data, offset, bytes, helper);
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
