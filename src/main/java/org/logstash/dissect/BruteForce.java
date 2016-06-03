package org.logstash.dissect;

public class BruteForce {

    public static int indexOf(byte[] text, int textStart, byte pattern) {
        for (int n = textStart; n < text.length; n++) {
            if (text[n] == pattern)
                return n;
        }
        return -1;
    }

    public static int indexOf(byte[] text, int textStart, byte pat1, byte pat2) {
        for (int n = textStart; n < text.length - 1; n++) {
            if (text[n] == pat1) {
                if (text[n + 1] == pat2)
                    return n;
            }
        }
        return -1;
    }
}
