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

    @Override
    public int indexOf(byte[] source, int fromIndex) {

        int sourceOffset = 0;
        int sourceCount = source.length;
        int targetOffset = 0;

        if (fromIndex >= sourceCount) {
            return (length == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (length == 0) {
            return fromIndex;
        }

        byte first = bytes[targetOffset];
        int max = sourceOffset + (sourceCount - length);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first byte. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first);
            }

            /* Found first byte, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + length - 1;
                for (int k = targetOffset + 1; j < end && source[j]
                        == bytes[k]; j++, k++);

                if (j == end) {
                    /* Found whole pattern. */
                    return i - sourceOffset;
                }
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
        final StringBuffer sb = new StringBuffer("DelimMany{");
        sb.append("string='").append(_string);
        sb.append("', length=").append(length);
        sb.append('}');
        return sb.toString();
    }
}
