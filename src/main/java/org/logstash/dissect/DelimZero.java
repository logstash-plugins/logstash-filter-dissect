package org.logstash.dissect;

class DelimZero implements Delim {
    private final byte b;
    private final String string;
    private final int length = 1;

    public DelimZero() {
        string = "";
        b = 0;
    }

    @Override
    public int indexOf(byte[] text, int textStart) {
        return -1;
    }

    @Override
    public int size() {
        return this.length;
    }

    @Override
    public String string() {
        return this.string;
    }

    @Override
    public String toString() {
        final StringBuffer sb = new StringBuffer("DelimZero{");
        sb.append("string='").append(string);
        sb.append("', length=").append(length);
        sb.append('}');
        return sb.toString();
    }
}
