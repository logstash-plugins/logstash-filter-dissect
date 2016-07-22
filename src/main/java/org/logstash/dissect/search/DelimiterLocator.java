package org.logstash.dissect.search;

public interface DelimiterLocator {
    /**
     * Search for a needle in a haystack.
     * Returns the integer position of where needle bytes was found in haystack bytes
     * Note: the position is where the first needle byte is.
     *
     * @param needle   an array of bytes that represents the text being located
     * @param haystack an array of bytes that represents the text being searched in
     * @param offset   a point inside the haystack to start the search from
     * @return         the position where the first byte of the needle was found
     */
    int indexOf(byte[] needle, byte[] haystack, int offset);
}
