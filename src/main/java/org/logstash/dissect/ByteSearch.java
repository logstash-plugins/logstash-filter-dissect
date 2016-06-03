package org.logstash.dissect;

import com.eaio.stringsearch.BoyerMooreHorspool;

public class ByteSearch {
    //    private static final BoyerMooreHorspool algo = new BoyerMooreHorspoolRaita();
    private static final BoyerMooreHorspool algo = new BoyerMooreHorspool();

    public static int indexOf(byte[] data, int offset, byte[] pattern,
                              Object helper) {

        return algo.searchBytes(data, offset, data.length, pattern, helper);
    }

    public static Object getHelper(byte[] pattern) {
        return algo.processBytes(pattern);
    }
}
