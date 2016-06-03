package org.logstash.dissect;

/**
 * The Knuth-Morris-Pratt Pattern Matching Algorithm for byte arrays.
 */
public class KMP {
    /**
     * Search the data byte array for the first occurrence of the byte array
     * pattern beginning at offset. Use '*' as a wildcard for any amount of
     * arbitrary bytes. A trailing wildcard will be ignored.
     */
    public static int indexOf(byte[] data, int offset, byte[] pattern,
                              int[] failure, boolean wildcard) {
        if (offset > data.length)
            return -1;

        int j = 0;

        boolean ignoreCase = false;

        for (int i = 0 + offset; i < data.length; i++) {
            if (wildcard && pattern[j] == '*') {
				/* Skip the wildcard */
                j++;

				/*
				 * If the wildcard was at the end of the pattern, data and
				 * pattern match and * can be ignored
				 */
                if (j == pattern.length)
                    return i - pattern.length;

				/*
				 * Go through the data and skip everything which is not
				 * pattern[j]
				 */
                while (i < data.length
                        && (!ignoreCase && pattern[j] != data[i] || ignoreCase
                        && pattern[j] != data[i]
                        && pattern[j] >= 'A'
                        && pattern[j] <= 'Z'
                        && pattern[j] + 32 != data[i]))
                    i++;

				/*
				 * The pattern[j] character wasn't found anywhere in the data so
				 * there is no match
				 */
                if (i == data.length)
                    return -1;
            }

            while (j > 0
                    && (!ignoreCase && pattern[j] != data[i] || ignoreCase
                    && pattern[j] != data[i] && pattern[j] >= 'A'
                    && pattern[j] <= 'Z' && pattern[j] + 32 != data[i])) {
                j = failure[j - 1];
            }

            if (pattern[j] == data[i]
                    || (ignoreCase && pattern[j] >= 'A' && pattern[j] <= 'Z' && pattern[j] + 32 == data[i])) {
                j++;
            }

            if (j == pattern.length) {
                return i - pattern.length + 1;
            }
        }
        return -1;
    }

    /**
     * Computes the failure function using a boot-strapping process, where the
     * pattern is matched against itself.
     * use this to pre-compute a failure for each unique delimiter pattern
     */
    public static int[] computeFailure(byte[] pattern) {
        int[] failure = new int[pattern.length];

        int j = 0;
        for (int i = 1; i < pattern.length; i++) {
            while (j > 0 && pattern[j] != pattern[i]) {
                j = failure[j - 1];
            }
            if (pattern[j] == pattern[i]) {
                j++;
            }
            failure[i] = j;
        }

        return failure;
    }
}
