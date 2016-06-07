package org.logstash.dissect;

import com.logstash.Event;
import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class DissectorTest {

    private Dissector subject(String map) {
        return new Dissector(map);
    }

    @Test
    public void testEmptyArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("")
                .dissect("".getBytes(), object);
        org.junit.Assert.assertEquals(0, object.size());
    }

    @Test
    public void testBasicArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b} %{c}")
                .dissect("foo bar   baz".getBytes(), object);
        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("  baz", object.get("c"));
    }

    @Test
    public void testMissingDelimBegin() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a}%{b} %{c}")
                .dissect("foo bar   baz".getBytes(), object);
        assertEquals(3, object.size());
        assertEquals("", object.get("a"));
        assertEquals("foo", object.get("b"));
        assertEquals("bar   baz", object.get("c"));
    }

    @Test
    public void testMissingDelimMiddle() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b}%{c} %{d}")
                .dissect("foo bar baz".getBytes(), object);
        assertEquals(4, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("", object.get("b"));
        assertEquals("bar", object.get("c"));
        assertEquals("baz", object.get("d"));
    }
    @Test
    public void testMissingDelimEnd() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b} %{c}%{d}")
                .dissect("foo bar baz quux".getBytes(), object);
        assertEquals(4, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("", object.get("c"));
        assertEquals("foo bar baz quux", object.get("d"));
    }

    @Test
    public void testBasicArgsWithSkip() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a} %{} %{c}")
                .dissect("foo bar baz".getBytes(), object);
        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("baz", object.get("c"));
    }

    @Test
    public void testAppendArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b} %{+b} %{z}")
                .dissect("foo bar baz quux".getBytes(), object);

        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar baz", object.get("b"));
        assertEquals("quux", object.get("z"));
    }

    @Test
    public void testRestArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a}------->%{b}")
                .dissect("foo------->bar baz quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar baz quux", object.get("b"));
    }
    @Test
    public void testEmptyRestArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a}------->%{}")
                .dissect("foo------->bar baz quux".getBytes(), object);

        assertEquals(1, object.size());
        assertEquals("foo", object.get("a"));
        assertFalse(object.containsValue("bar baz quux"));
     }

    @Test
    public void testUnicodeDelim() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a} » %{b}")
                .dissect("foo » bar baz quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar baz quux", object.get("b"));
    }

    @Test
    public void testRestAppendArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b} %{+a}")
                .dissect("foo bar baz quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo baz quux", object.get("a"));
        assertEquals("bar", object.get("b"));
    }

    @Test
    public void testReorderAppendArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{+a} %{a} %{+a} %{b}")
                .dissect("December 31 1999 quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("31 December 1999", object.get("a"));
        assertEquals("quux", object.get("b"));
    }

    @Test
    public void testReorderAppendArgsWithOrdinals() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{+a/2} %{+a/4} %{+a/1} %{+a/3}")
                .dissect("bar quux foo baz".getBytes(), object);

        assertEquals(1, object.size());
        assertEquals("foo bar baz quux", object.get("a"));
    }

    @Test
    public void testOneAppendArgWhenFieldMissing() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{+a} %{b}")
                .dissect("foo bar".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
    }

    @Test
    public void testAppendArgsWhenFieldMissing() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{+a} %{b} %{+a} %{c}")
                .dissect("foo bar baz quux".getBytes(), object);

        assertEquals(3, object.size());
        assertEquals("foo baz", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("quux", object.get("c"));
    }

    @Test
    public void testNonFieldStartArgs() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("[%{a}] %{b} %{c}")
                .dissect("[foo bar] baz quux".getBytes(), object);

        assertEquals(3, object.size());
        assertEquals("foo bar", object.get("a"));
        assertEquals("baz", object.get("b"));
        assertEquals("quux", object.get("c"));
    }

    @Test
    public void testIndirectFields() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{k1}=%{&k1}, %{k2}=%{&k2}")
                .dissect("foo=bar, baz=quux".getBytes(), object);

        assertEquals(4, object.size());
        assertEquals("foo", object.get("k1"));
        assertEquals("baz", object.get("k2"));
        assertEquals("bar", object.get("foo"));
        assertEquals("quux", object.get("baz"));
    }

    @Test
    public void testIndirectFieldsMissing() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{k1}=%{&k3}, %{k2}=%{&k4}")
                .dissect("foo=bar, baz=quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo", object.get("k1"));
        assertEquals("baz", object.get("k2"));
    }

    @Test
    public void testIndirectFieldsSkipsMissing() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{?k1}=%{&k3}, %{?k2}=%{&k4}")
                .dissect("foo=bar, baz=quux".getBytes(), object);

        assertEquals(0, object.size());
    }

    @Test
    public void testIndirectFieldsWithSkipSources() throws Exception {
        Map<String, Object> object = new HashMap<>();
        subject("%{?k1}=%{&k1}, %{?k2}=%{&k2}")
                .dissect("foo=bar, baz=quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("bar", object.get("foo"));
        assertEquals("quux", object.get("baz"));
    }

    @Test
    public void testIndirectFieldsWithMapSources() throws Exception {
        Map<String, Object> object = new HashMap<>();
        object.put("k1", "a");
        object.put("k2", "b");
        object.put("k3", "c");
        subject("%{&k1}, %{&k2}, %{&k3}")
                .dissect("foo, bar, baz".getBytes(), object);

        assertEquals(6, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("baz", object.get("c"));
    }

    @Test
    public void testIndirectFieldsWithEventSources() throws Exception {
        Event object = new Event();
        object.setField("k1", "a");
        object.setField("k2", "b");
        object.setField("k3", "c");
        subject("%{&k1}, %{&k2}, %{&k3}")
                .dissect("foo, bar, baz".getBytes(), object);

        assertTrue(object.getData().size() >= 6);
        assertEquals("foo", object.getField("a"));
        assertEquals("bar", object.getField("b"));
        assertEquals("baz", object.getField("c"));
    }

    @Test
    public void testComplex() throws Exception {
        String src = "42 2016-05-25T14:47:23Z host.name.com RT_FLOW - RT_FLOW_SESSION_DENY: session denied 2.2.2.20/60000->1.1.1.10/8090 None 6(0) DEFAULT-DENY ZONE-UNTRUST ZONE-DMZ UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0";
        String mpp = "%{} %{syslog_timestamp} %{hostname} %{rt}: %{reason} %{+reason} %{src_ip}/%{src_port}->%{dst_ip}/%{dst_port} %{polrt} %{+polrt} %{+polrt} %{from_zone} %{to_zone} %{rest}";
        Map<String, Object> object = new HashMap<>();
        subject(mpp)
                .dissect(src.getBytes(), object);
        assertEquals(12, object.size());
        assertEquals("2016-05-25T14:47:23Z", object.get("syslog_timestamp"));
        assertEquals("host.name.com", object.get("hostname"));
        assertEquals("RT_FLOW - RT_FLOW_SESSION_DENY", object.get("rt"));
        assertEquals("session denied", object.get("reason"));
        assertEquals("2.2.2.20", object.get("src_ip"));
        assertEquals("60000", object.get("src_port"));
        assertEquals("1.1.1.10", object.get("dst_ip"));
        assertEquals("8090", object.get("dst_port"));
        assertEquals("None 6(0) DEFAULT-DENY", object.get("polrt"));
        assertEquals("ZONE-UNTRUST", object.get("from_zone"));
        assertEquals("ZONE-DMZ", object.get("to_zone"));
        assertEquals("UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0", object.get("rest"));
    }

    @Test
    public void testComplexWithEvent() throws Exception {
        String src = "42 2016-05-25T14:47:23Z host.name.com RT_FLOW - RT_FLOW_SESSION_DENY: session denied 2.2.2.20/60000->1.1.1.10/8090 None 6(0) DEFAULT-DENY ZONE-UNTRUST ZONE-DMZ UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0";
        String mpp = "%{} %{syslog_timestamp} %{hostname} %{rt}: %{reason} %{+reason} %{src_ip}/%{src_port}->%{dst_ip}/%{dst_port} %{polrt} %{+polrt} %{+polrt} %{from_zone} %{to_zone} %{rest}";
        Event object = new Event();
        subject(mpp)
                .dissect(src.getBytes(), object);
        assertTrue(object.getData().size() >= 12);
        assertEquals("2016-05-25T14:47:23Z", object.getField("syslog_timestamp"));
        assertEquals("host.name.com", object.getField("hostname"));
        assertEquals("RT_FLOW - RT_FLOW_SESSION_DENY", object.getField("rt"));
        assertEquals("session denied", object.getField("reason"));
        assertEquals("2.2.2.20", object.getField("src_ip"));
        assertEquals("60000", object.getField("src_port"));
        assertEquals("1.1.1.10", object.getField("dst_ip"));
        assertEquals("8090", object.getField("dst_port"));
        assertEquals("None 6(0) DEFAULT-DENY", object.getField("polrt"));
        assertEquals("ZONE-UNTRUST", object.getField("from_zone"));
        assertEquals("ZONE-DMZ", object.getField("to_zone"));
        assertEquals("UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0", object.getField("rest"));
    }

}