package org.logstash.dissect;

import org.junit.Test;

public class StringSearchTest {
    @Test
    public void testIndexOfOneByteSearch() throws Exception {
        byte[] source = "aaa bbb ccc ddd".getBytes();
        Delimiter delimiter = DelimiterFactory.create(" ");
        int p = delimiter.indexOf(source, 0);
        org.junit.Assert.assertEquals(3, p);
    }

    @Test
    public void testIndexOfOneByteSearchOffset() throws Exception {
        byte[] source = "aaa bbb ccc ddd".getBytes();
        Delimiter delimiter = DelimiterFactory.create(" ");
        int p = delimiter.indexOf(source, 4);
        org.junit.Assert.assertEquals(7, p);
    }

    @Test
    public void testIndexOfTwoBytesSearch() throws Exception {
        byte[] source = "aaa..bbb...ccc..ddd".getBytes();
        Delimiter delimiter = DelimiterFactory.create("..");
        int p = delimiter.indexOf(source, 0);
        org.junit.Assert.assertEquals(3, p);
    }
    @Test
    public void testIndexOfTwoBytesSearchOffset() throws Exception {
        byte[] source = "aaa..bbb.ccc..ddd".getBytes();
        Delimiter delimiter = DelimiterFactory.create("..");
        int p = delimiter.indexOf(source, 5);
        org.junit.Assert.assertEquals(12, p);
    }

    @Test
    public void testIndexOfBytesSearchOffset() throws Exception {
        byte[] source = "ddd-->eee-->fff-->ggg".getBytes();
        Delimiter delimiter = DelimiterFactory.create("-->");
        int p = delimiter.indexOf(source, 5);
        org.junit.Assert.assertEquals(9, p);
    }

    @Test
    public void testIndexOfBytesSearchBegin() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        Delimiter delimiter = DelimiterFactory.create("-->");
        int p = delimiter.indexOf(source, 0);
        org.junit.Assert.assertEquals(0, p);
    }

    @Test
    public void testIndexOfBytesSearchBeginOffset() throws Exception {
        byte[] source = "-->-->fff-->ggg".getBytes();
        Delimiter delimiter = DelimiterFactory.create("-->");
        int p = delimiter.indexOf(source, 3);
        org.junit.Assert.assertEquals(3, p);
    }

}