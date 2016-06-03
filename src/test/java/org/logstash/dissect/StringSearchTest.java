package org.logstash.dissect;

import org.junit.Test;

public class StringSearchTest {
    @Test
    public void testBasicKMPSearch() throws Exception {
        byte[] source = "aaa bbb ccc ddd".getBytes();
        byte[] delim = " ".getBytes();
        int[] fail = KMP.computeFailure(delim);
        int p = KMP.indexOf(source, 0, delim, fail, false);
        org.junit.Assert.assertEquals(3, p);
    }

    @Test
    public void testBasicRaitaSearch() throws Exception {
        byte[] source = "aaa bbb ccc ddd".getBytes();
        byte[] delim = " ".getBytes();
        Object helper = ByteSearch.getHelper(delim);
        int p = ByteSearch.indexOf(source, 0, delim, helper);
        org.junit.Assert.assertEquals(3, p);
    }

    @Test
    public void testBasicRaitaSearchOffset() throws Exception {
        byte[] source = "ddd-->eee-->fff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        Object helper = ByteSearch.getHelper(delim);
        int p = ByteSearch.indexOf(source, 5, delim, helper);
        org.junit.Assert.assertEquals(9, p);
    }

    @Test
    public void testBasicSearchOffset() throws Exception {
        byte[] source = "ddd-->eee-->fff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        int[] fail = KMP.computeFailure(delim);
        int p = KMP.indexOf(source, 6, delim, fail, false);
        org.junit.Assert.assertEquals(9, p);
    }

    @Test
    public void testSearchBegin() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        int[] fail = KMP.computeFailure(delim);
        int p = KMP.indexOf(source, 0, delim, fail, false);
        org.junit.Assert.assertEquals(0, p);
    }

    @Test
    public void testSearchBeginOffset() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        int[] fail = KMP.computeFailure(delim);
        int p = KMP.indexOf(source, 3, delim, fail, false);
        org.junit.Assert.assertEquals(3, p);
    }

    @Test
    public void testSearchMove() throws Exception {
        byte[] source = "-->-->ffff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        int[] fail = KMP.computeFailure(delim);
        int l = 0;
        int p = 0;
        int i = 0;
        boolean found = false;
        while (!found) {
            i++;
            p = KMP.indexOf(source, l, delim, fail, false);
            if (p == l) {
                l = p + delim.length;
            } else {
                found = true;
            }
            if (i > 5) break;
        }
        String value = new String(source, l, p - l);

        org.junit.Assert.assertEquals("expect 'l':", 6, l);
        org.junit.Assert.assertEquals("expect 'p':", 10, p);
        org.junit.Assert.assertEquals("expect 'value':", "ffff", value);
    }
}