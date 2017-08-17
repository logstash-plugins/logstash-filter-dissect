package org.logstash.dissect.search;

public class MultiByteLocator implements DelimiterLocator {
    public static final MultiByteLocator INSTANCE = new MultiByteLocator();

    private MultiByteLocator() {
    }

    @Override
    public int indexOf(final byte[] needle, final byte[] haystack, final int offset) {
        int localOffset = offset;

        final int sourceOffset = 0;
        final int sourceCount = haystack.length;
        final int targetOffset = 0;

        if (localOffset >= sourceCount) {
            return -1;
        }
        if (localOffset < 0) {
            localOffset = 0;
        }

        final byte first = needle[targetOffset];
        final int max = sourceOffset + (sourceCount - needle.length);

        for (int i = sourceOffset + localOffset; i <= max; i++) {
            /* Look for first byte. */
            if (haystack[i] != first) {
                while (++i <= max && haystack[i] != first) {
                    // do nothing
                }
            }

            /* Found first byte, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                final int end = j + needle.length - 1;
                for (int k = targetOffset + 1; j < end && haystack[j]
                        == needle[k]; j++, k++) {
                    // do nothing
                }

                if (j == end) {
                    /* Found whole pattern. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
}
