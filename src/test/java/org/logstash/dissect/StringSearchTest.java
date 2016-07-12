package org.logstash.dissect;

import org.junit.Test;

public class StringSearchTest {
    @Test
    public void testIndexOfBytesSearch() throws Exception {
        byte[] source = "aaa bbb ccc ddd".getBytes();
        Delim delim = new DelimMany(" ");
        int p = delim.indexOf(source, 0);
        org.junit.Assert.assertEquals(3, p);
    }

    @Test
    public void testIndexOfBytesSearchOffset() throws Exception {
        byte[] source = "ddd-->eee-->fff-->ggg".getBytes();
        Delim delim = new DelimMany("-->");
        int p = delim.indexOf(source, 5);
        org.junit.Assert.assertEquals(9, p);
    }

    @Test
    public void testIndexOfBytesSearchBegin() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        Delim delim = new DelimMany("-->");
        int p = delim.indexOf(source, 0);
        org.junit.Assert.assertEquals(0, p);
    }

    @Test
    public void testIndexOfBytesSearchBeginOffset() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        Delim delim = new DelimMany("-->");
        int p = delim.indexOf(source, 3);
        org.junit.Assert.assertEquals(3, p);
    }

}