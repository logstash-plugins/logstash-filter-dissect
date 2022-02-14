package org.logstash.dissect;


import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.logstash.Event;
import org.logstash.dissect.fields.InvalidFieldException;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;

public class DissectorTest {

    private Dissector subject(final String map) {
        return Dissector.create(map);
    }

    @Rule
    public ExpectedException exception = ExpectedException.none();

    @Test
    @Ignore("Skipping scratch pad Java 8 streaming experiment")
    public void scratch() throws Exception {
        try {
            subject("");
        } catch (Throwable ex) {
            String est = String.join(", ", Arrays.stream(ex.getStackTrace()).limit(4).map(StackTraceElement::toString).toArray(String[]::new));
            System.out.println(est);
        }
    }

    @Test
    public void testEmptyArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        exception.expect(IllegalArgumentException.class);
        exception.expectMessage("The mapping string cannot be empty");
        subject("");
    }

    @Test
    public void testBasicArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        final String source = "foo bar   baz";
        final DissectResult result = subject("%{a} %{b->} %{c}").dissect(source.getBytes(), object);
        assertThat(object.size(), is(equalTo(3)));
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("baz", object.get("c"));
        assertTrue(result.matched());
    }

    @Test
    public void testNullSource() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        final DissectResult result = subject("%{a}%{b} %{c}").dissect(null, object);
        assertTrue(result.notMatched());
    }

    @Test
    public void testMissingDelimBegin() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        final DissectResult result = subject("%{a}%{b} %{c}")
                .dissect("foo bar   baz".getBytes(), object);
        assertTrue(result.notMatched());
    }

    @Test
    public void testMissingDelimMiddle() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        DissectResult result = subject("%{a} %{b}%{c} %{d}")
                .dissect("foo bar baz".getBytes(), object);
        assertTrue(result.notMatched());
    }

    @Test
    public void testMissingDelimEnd() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        DissectResult result = subject("%{a} %{b} %{c}%{d}")
                .dissect("foo bar baz quux".getBytes(), object);
        assertTrue(result.notMatched());
    }

    @Test
    public void testBasicArgsWithSkip() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a} %{} %{c}")
                .dissect("foo bar baz".getBytes(), object);
        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("baz", object.get("c"));
    }

    @Test
    public void testAppendArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b} %{+b} %{z}")
                .dissect("foo bar baz quux".getBytes(), object);

        assertEquals(3, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar baz", object.get("b"));
        assertEquals("quux", object.get("z"));
    }

    @Test
    public void testRestArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a}------->%{b}")
                .dissect("foo------->bar baz quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar baz quux", object.get("b"));
    }
    @Test
    public void testEmptyRestArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a}------->%{}")
                .dissect("foo------->bar baz quux".getBytes(), object);

        assertEquals(1, object.size());
        assertEquals("foo", object.get("a"));
        assertFalse(object.containsValue("bar baz quux"));
     }

    @Test
    @Ignore("https://github.com/logstash-plugins/logstash-filter-dissect/issues/23")
    public void testUnicodeDelim() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a} » %{b}»%{c}€%{d}")
                .dissect("foo » bar»baz€quux".getBytes("UTF-8"), object);

        assertEquals(4, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("baz", object.get("c"));
        assertEquals("quux", object.get("d"));
    }

    @Test
    public void testRestAppendArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b} %{+a}")
                .dissect("foo bar baz quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo baz quux", object.get("a"));
        assertEquals("bar", object.get("b"));
    }

    @Test
    public void testReorderAppendArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{+a} %{a} %{+a} %{b}")
                .dissect("December 31 1999 quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("31 December 1999", object.get("a"));
        assertEquals("quux", object.get("b"));
    }

    @Test
    public void testReorderAppendArgsWithOrdinals() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{+a/2} %{+a/4} %{+a/1} %{+a/3}")
                .dissect("bar quux foo baz".getBytes(), object);

        assertEquals(1, object.size());
        assertEquals("foo bar baz quux", object.get("a"));
    }

    @Test
    public void testOneAppendArgWhenFieldMissing() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{+a} %{b}")
                .dissect("foo bar".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
    }

    @Test
    public void testAppendArgsWhenFieldMissing() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{+a} %{b} %{+a} %{c}")
                .dissect("foo bar baz quux".getBytes(), object);

        assertEquals(3, object.size());
        assertEquals("foo baz", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("quux", object.get("c"));
    }

    @Test
    public void testNonFieldStartArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("[%{a}] %{b} %{c}")
                .dissect("[foo bar] baz quux".getBytes(), object);

        assertEquals(3, object.size());
        assertEquals("foo bar", object.get("a"));
        assertEquals("baz", object.get("b"));
        assertEquals("quux", object.get("c"));
    }

    @Test
    public void testIndirectFields() throws Exception {
        final Map<String, Object> object = new HashMap<>();
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
        final Map<String, Object> object = new HashMap<>();
        subject("%{k1}=%{&k3}, %{k2}=%{&k4}")
                .dissect("foo=bar, baz=quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("foo", object.get("k1"));
        assertEquals("baz", object.get("k2"));
    }

    @Test
    public void testIndirectFieldsSkipsMissing() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{?k1}=%{&k3}, %{?k2}=%{&k4}")
                .dissect("foo=bar, baz=quux".getBytes(), object);

        assertEquals(0, object.size());
    }

    @Test
    public void testIndirectFieldsWithSkipSources() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{?k1}=%{&k1}, %{?k2}=%{&k2}")
                .dissect("foo=bar, baz=quux".getBytes(), object);

        assertEquals(2, object.size());
        assertEquals("bar", object.get("foo"));
        assertEquals("quux", object.get("baz"));
    }

    @Test
    public void testIndirectFieldsWithMapSources() throws Exception {
        final Map<String, Object> object = new HashMap<>();
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
        final Event object = new Event();
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
        final String src = "42 2016-05-25T14:47:23Z host.name.com RT_FLOW - RT_FLOW_SESSION_DENY: session denied 2.2.2.20/60000->1.1.1.10/8090 None 6(0) DEFAULT-DENY ZONE-UNTRUST ZONE-DMZ UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0";
        final String mpp = "%{} %{syslog_timestamp} %{hostname} %{rt}: %{reason} %{+reason} %{src_ip}/%{src_port}->%{dst_ip}/%{dst_port} %{polrt} %{+polrt} %{+polrt} %{from_zone} %{to_zone} %{rest}";
        final Map<String, Object> object = new HashMap<>();
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
        final String src = "42 2016-05-25T14:47:23Z host.name.com RT_FLOW - RT_FLOW_SESSION_DENY: session denied 2.2.2.20/60000->1.1.1.10/8090 None 6(0) DEFAULT-DENY ZONE-UNTRUST ZONE-DMZ UNKNOWN UNKNOWN N/A(N/A) ge-0/0/0.0";
        final String mpp = "%{} %{syslog_timestamp} %{hostname} %{rt}: %{reason} %{+reason} %{src_ip}/%{src_port}->%{dst_ip}/%{dst_port} %{polrt} %{+polrt} %{+polrt} %{from_zone} %{to_zone} %{rest}";
        final Event object = new Event();
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

    @Test
    public void testInvalidAppendField() {
        final String mpp = "%{+/2}";
        exception.expect(InvalidFieldException.class);
        exception.expectMessage("Field cannot be a prefix and a suffix without a name section");
        subject(mpp);
    }

    @Test
    public void testInvalidAppendIndirectField() {
        final String mpp = "%{+&a_field}";
        exception.expect(InvalidFieldException.class);
        exception.expectMessage("Field cannot prefix with both Append and Indirect Prefix (+&): +&a_field");
        subject(mpp);
    }

    @Test
    public void testInvalidIndirectAppendField() {
        final String mpp = "%{&+a_field}";
        exception.expect(InvalidFieldException.class);
        exception.expectMessage("Field cannot prefix with both Append and Indirect Prefix (&+): &+a_field");
        subject(mpp);
    }

    @Test
    public void testRepeatDelimsArgs() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a->}   %{b->}---%{c}")
                .dissect("foo            bar------------baz".getBytes(), object);
        assertThat(object.size(), is(equalTo(3)));
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("baz", object.get("c"));
    }

    @Test
    public void testLeadingDelimiters() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{?->}-%{a}")
                .dissect("-----666".getBytes(), object);
        assertEquals("666", object.get("a"));
    }

    @Test
    public void testMissingFieldData() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a},%{b},%{c},%{d},%{e},%{f}")
                .dissect("111,,333,,555,666".getBytes(), object);
        assertEquals("111", object.get("a"));
        assertEquals("", object.get("b"));
        assertEquals("333", object.get("c"));
        assertEquals("", object.get("d"));
        assertEquals("555", object.get("e"));
        assertEquals("666", object.get("f"));
    }

    // https://github.com/logstash-plugins/logstash-filter-dissect/issues/46
    @Test
    public void testMultibyteCharacterStrings() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        final byte[] bytes = "⟳༒.࿏.༒⟲".getBytes();
        final DissectResult result = subject("%{a}.࿏.%{b}").dissect(bytes, object);
        assertTrue(result.matched());
        assertEquals("⟳༒", object.get("a"));
        assertEquals("༒⟲", object.get("b"));
    }

    @Test
    public void testSingleMultibyteCharacterString() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        final byte[] bytes = "子".getBytes();
        final DissectResult result = subject("%{a}").dissect(bytes, object);
        assertTrue(result.matched());
        assertEquals("子", object.get("a"));
    }

    // https://github.com/logstash-plugins/logstash-filter-dissect/issues/42
    // https://github.com/logstash-plugins/logstash-filter-dissect/issues/47
    @Test
    public void testNewlineInDelim() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a}{\n}%{b}")
                .dissect("aaa{\n}bbb".getBytes(), object);
        assertEquals("aaa", object.get("a"));
        assertEquals("bbb", object.get("b"));
    }

    @Test
    public void testStartingDelimInMiddle() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        DissectResult result = subject("MACHINE[%{a}] %{b}")
                .dissect("1234567890 MACHINE[foo] bar".getBytes(), object);
        assertTrue(result.notMatched());
    }

    @Test
    public void testNoMatchAtAll() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("%{a} %{b} %{c}")
                .dissect("foo:bar:baz".getBytes(), object);
        assertEquals(true, object.isEmpty());
    }

    @Test
    public void testTrailingDelimiter() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        subject("/var/%{key1}/log/%{key2}.log")
                .dissect("/var/foo/log/bar.log".getBytes(), object);
        assertEquals("foo", object.get("key1"));
        assertEquals("bar", object.get("key2"));
    }

    @Test
    @Ignore("Skipping long running test used only when profiling")
    public void profileRepeatDelimsArgs() throws Exception {
        Map<String, Object> object= new HashMap<>();
        long start = System.currentTimeMillis();
        Dissector dissector = subject("%{a->}   %{b}-.-%{c}-%{d}-..-%{e}-%{f}-%{g}-%{h}");
        byte[] bytes = "foo            bar-.-baz-1111-..-22-333-4444-55555".getBytes();
        while (true) {
            dissector.dissect(bytes, object);
            if(System.currentTimeMillis() - start > 240000) break;
            object.clear();
        }
        assertThat(object.size(), is(equalTo(8)));
        assertEquals("foo", object.get("a"));
        assertEquals("bar", object.get("b"));
        assertEquals("baz", object.get("c"));
    }

    @Test
    public void testPaddingWithTextEnd() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        final String source = "XXX YYY ZZZ";
        final DissectResult result = subject("XXX %{y->} ZZZ").dissect(source.getBytes(), object);
        assertThat(object.size(), is(equalTo(1)));
        assertEquals("YYY", object.get("y"));
        assertTrue(result.matched());
    }

    @Test
    public void testPaddingFollowingFieldEnd() throws Exception {
        final Map<String, Object> object = new HashMap<>();
        final String source = "XXX YYY ZZZ";
        final DissectResult result = subject("XXX %{y->} %{z}").dissect(source.getBytes(), object);
        assertThat(object.size(), is(equalTo(2)));
        assertEquals("YYY", object.get("y"));
        assertEquals("ZZZ", object.get("z"));
        assertTrue(result.matched());
    }
}
