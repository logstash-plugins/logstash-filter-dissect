package org.logstash.dissect;

import org.junit.Assert;
import org.junit.Test;

public class StringSearchTest {
    @Test
    public void testIndexOfOneByteSearch() throws Exception {
        final byte[] source = "aaa bbb ccc ddd".getBytes();
        final Delimiter delimiter = Delimiter.create(" ");
        final int p = delimiter.indexOf(source, 0);
        Assert.assertEquals(3, p);
    }

    @Test
    public void testIndexOfOneByteSearchOffset() throws Exception {
        final byte[] source = "aaa bbb ccc ddd".getBytes();
        final Delimiter delimiter = Delimiter.create(" ");
        final int p = delimiter.indexOf(source, 4);
        Assert.assertEquals(7, p);
    }

    @Test
    public void testIndexOfTwoBytesSearch() throws Exception {
        final byte[] source = "aaa..bbb...ccc..ddd".getBytes();
        final Delimiter delimiter = Delimiter.create("..");
        final int p = delimiter.indexOf(source, 0);
        Assert.assertEquals(3, p);
    }
    @Test
    public void testIndexOfTwoBytesSearchOffset() throws Exception {
        final byte[] source = "aaa..bbb.ccc..ddd".getBytes();
        final Delimiter delimiter = Delimiter.create("..");
        final int p = delimiter.indexOf(source, 5);
        Assert.assertEquals(12, p);
    }

    @Test
    public void testIndexOfBytesSearchOffset() throws Exception {
        final byte[] source = "ddd-->eee-->fff-->ggg".getBytes();
        final Delimiter delimiter = Delimiter.create("-->");
        final int p = delimiter.indexOf(source, 5);
        Assert.assertEquals(9, p);
    }

    @Test
    public void testIndexOfBytesSearchBegin() throws Exception {
        final byte[] source = "-->-->fff-->ggg".getBytes();
        final Delimiter delimiter = Delimiter.create("-->");
        final int p = delimiter.indexOf(source, 0);
        Assert.assertEquals(0, p);
    }

    @Test
    public void testIndexOfBytesSearchBeginOffset() throws Exception {
        final byte[] source = "-->-->fff-->ggg".getBytes();
        final Delimiter delimiter = Delimiter.create("-->");
        final int p = delimiter.indexOf(source, 3);
        Assert.assertEquals(3, p);
    }

    @Test
    public void testIndexOfMultiBytesSearch() throws Exception {
        final byte[] source = "0123456-->-->-->ggg".getBytes();
        final Delimiter delimiter = Delimiter.create("-->-->-->");
        final int p = delimiter.indexOf(source, 3);
        Assert.assertEquals(7, p);
    }

}