package org.logstash.dissect.search;

public class MultiByteLocator implements DelimiterLocator {
    public static MultiByteLocator INSTANCE = new MultiByteLocator();

    private MultiByteLocator() {
    }

    @Override
    public int indexOf(byte[] needle, byte[] haystack, int offset) {
        int localOffset = offset;

        int sourceOffset = 0;
        int sourceCount = haystack.length;
        int targetOffset = 0;

        if (localOffset >= sourceCount) {
            return (needle.length == 0 ? sourceCount : -1);
        }
        if (localOffset < 0) {
            localOffset = 0;
        }
        if (needle.length == 0) {
            return localOffset;
        }

        byte first = needle[targetOffset];
        int max = sourceOffset + (sourceCount - needle.length);

        for (int i = sourceOffset + localOffset; i <= max; i++) {
            /* Look for first byte. */
            if (haystack[i] != first) {
                while (++i <= max && haystack[i] != first);
            }

            /* Found first byte, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + needle.length - 1;
                for (int k = targetOffset + 1; j < end && haystack[j]
                        == needle[k]; j++, k++);

                if (j == end) {
                    /* Found whole pattern. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
}
