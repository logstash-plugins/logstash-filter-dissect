package org.logstash.dissect;

import org.junit.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.Assert.assertEquals;

public class DissectTest {

    private Dissect subject(String src, String map) {
        return new Dissect(src, map);
    }

    @Test
    public void testEmptyArgs() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject("", "").dissect(object);
        org.junit.Assert.assertEquals(0, object.size());
    }

    @Test
    public void testBasicArgs() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "foo bar baz",
                "%{a} %{b} %{c}"
        ).dissect(object);
        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("baz", object.get("c"));
    }

    @Test
    public void testBasicArgsWithSkip() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "foo bar baz",
                "%{a} %{} %{z}"
        ).dissect(object);
        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("baz", object.get("z"));
    }

    @Test
    public void testBangArgs() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "foo bar baz quux",
                "%{a} %{b} %{b!} %{z}"
        ).dissect(object);
        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar baz", object.get("b"));
        assertEquals("quux", object.get("z"));
    }

    @Test
    public void testRestArgs() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "foo bar baz quux",
                "%{a} %{b}"
        ).dissect(object);
        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar baz quux", object.get("b"));
    }

    @Test
    public void testRestBangArgs() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "foo bar baz quux",
                "%{a} %{b} %{a!}"
        ).dissect(object);
        assertEquals(2, object.size());
        assertEquals("foo baz quux", object.get("a"));
        assertEquals("bar", object.get("b"));
    }

    @Test
    public void testNonFieldStartArgs() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "[foo bar] baz quux",
                "[%{a}] %{b} %{c}"
        ).dissect(object);
        assertEquals(3, object.size());
        assertEquals("foo bar", object.get("a"));
        assertEquals("baz", object.get("b"));
        assertEquals("quux", object.get("c"));
    }

    @Test
    public void testComplex() throws Exception {
        String src = "42 2016-05-25T14:47:23Z host.name.com RT_FLOW - RT_FLOW_SESSION_DENY: session denied 2.2.2.20/60000->1.1.1.10/8090 None 6(0) DEFAULT-DENY ZONE-UNTRUST ZONE-DMZ UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0";
        String mpp = "%{} %{syslog_timestamp} %{hostname} %{rt}: %{reason} %{reason!} %{src_ip}/%{src_port}->%{dst_ip}/%{dst_port} %{polrt} %{polrt!} %{polrt!} %{from_zone} %{to_zone} %{rest}";
        Map<String, Object> object = new HashMap<String, Object>();
        subject(src, mpp).dissect(object);
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
    public void testAppendToMapHavingEntries() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        object.put("roles", "admin");

        subject(
                "foo editor bar",
                "%{a} %{roles!} %{b}"
        ).dissect(object);
        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("admin editor", object.get("roles"));
    }

    @Test
    public void testSourceMultipleSpacesRest() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "foo editor      bar",
                "%{a} %{role} %{b}"
        ).dissect(object);
        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("editor", object.get("role"));
        assertEquals("     bar", object.get("b"));
    }

    @Test
    public void testSourceMultipleSpacesMid() throws Exception {
        Map<String, Object> object = new HashMap<String, Object>();
        subject(
                "foo   editor bar",
                "%{a} %{role} %{b}"
        ).dissect(object);
        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("editor", object.get("role"));
        assertEquals("bar", object.get("b"));
    }
}