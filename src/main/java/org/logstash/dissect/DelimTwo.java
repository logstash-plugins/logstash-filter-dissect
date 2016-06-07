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

    @Override
    public int indexOf(byte[] text, int textStart) {
        for (int n = textStart; n < text.length - 1; n++) {
            if (text[n] == b1) {
                if (text[n + 1] == b2)
                    return n;
            }
        }
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
        return "'" + _string + "'";
    }
}
