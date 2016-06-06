package org.logstash.dissect;

import org.junit.Test;

public class StringSearchTest {
    @Test
    public void testIndexOfBytesSearch() throws Exception {
        byte[] source = "aaa bbb ccc ddd".getBytes();
        byte[] delim = " ".getBytes();
        int p = IndexOfBytes.indexOf(source, 0, delim);
        org.junit.Assert.assertEquals(3, p);
    }

    @Test
    public void testIndexOfBytesSearchOffset() throws Exception {
        byte[] source = "ddd-->eee-->fff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        int p = IndexOfBytes.indexOf(source, 5, delim);
        org.junit.Assert.assertEquals(9, p);
    }

    @Test
    public void testIndexOfBytesSearchBegin() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        int p = IndexOfBytes.indexOf(source, 0, delim);
        org.junit.Assert.assertEquals(0, p);
    }

    @Test
    public void testIndexOfBytesSearchBeginOffset() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        byte[] delim = "-->".getBytes();
        int p = IndexOfBytes.indexOf(source, 3, delim);
        org.junit.Assert.assertEquals(3, p);
    }

}