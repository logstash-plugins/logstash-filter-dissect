package org.logstash.dissect;

public class IndexOfBytes {
    public static   int indexOf(byte[] source, int fromIndex, byte[] pattern) {

        int sourceOffset = 0;
        int sourceCount = source.length;
        int targetOffset = 0;
        int targetCount = pattern.length;

        if (fromIndex >= sourceCount) {
            return (targetCount == 0 ? sourceCount : -1);
        }
        if (fromIndex < 0) {
            fromIndex = 0;
        }
        if (targetCount == 0) {
            return fromIndex;
        }

        byte first = pattern[targetOffset];
        int max = sourceOffset + (sourceCount - targetCount);

        for (int i = sourceOffset + fromIndex; i <= max; i++) {
            /* Look for first byte. */
            if (source[i] != first) {
                while (++i <= max && source[i] != first);
            }

            /* Found first byte, now look at the rest of v2 */
            if (i <= max) {
                int j = i + 1;
                int end = j + targetCount - 1;
                for (int k = targetOffset + 1; j < end && source[j]
                        == pattern[k]; j++, k++);

                if (j == end) {
                    /* Found whole pattern. */
                    return i - sourceOffset;
                }
            }
        }
        return -1;
    }
}
