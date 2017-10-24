package org.logstash.dissect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.logstash.Event;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.HashMap;

import static org.assertj.core.api.Assertions.assertThat;

public class ConverterTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Event subject(final String src, final Object val) {
        final Event e = new Event();
        e.setField(src, val);
        return e;
    }

    @Test
    public void convertEventFieldToInt() {
        final String src = "[foo]";
        final Event e = subject(src, "1234");
        Converters.select("int").convert(e, src);
        Object actual = e.getField(src);
        assertThat(actual).isEqualTo(new BigInteger("1234"));
    }

    @Test
    public void convertEventFieldToIntFromDoubleString() {
        final String src = "[foo]";
        final Event e = subject(src, "12.34");
        Converters.select("int").convert(e, src);
        Object actual = e.getField(src);
        assertThat(actual).isEqualTo(new BigInteger("12"));
    }

    @Test
    public void convertEventFieldToIntFromDoubleObject() {
        final String src = "[foo]";
        final Event e = subject(src, 12.34);
        Converters.select("int").convert(e, src);
        Object actual = e.getField(src);
        assertThat(actual).isEqualTo(new BigInteger("12"));
    }

    @Test
    public void convertEventFieldToIntFromBiiiigIntString() {
        final String src = "[foo]";
        final String value = "439474042575071862892";
        final Event e = subject(src, value);
        Converters.select("int").convert(e, src);
        assertThat(e.getField(src)).isEqualTo(new BigInteger(value));
    }

    @Test
    public void convertEventFieldToIntFromIncompatibleObject() {
        final String src = "[foo]";
        final Event e = subject(src, new HashMap());
        exception.expect(NumberFormatException.class);
        Converters.select("int").convert(e, src);
    }

    @Test
    public void convertEventFieldToDouble() {
        final String src = "[foo]";
        final Event e = subject(src, "12.34");
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src)).isEqualTo(new BigDecimal("12.34"));
    }

    @Test
    public void convertEventFieldToDoubleFromIntString() {
        final String src = "[foo]";
        final Event e = subject(src, "1234");
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src)).isEqualTo(new BigDecimal("1234"));
    }

    @Test
    public void convertEventFieldToDoubleFromIntObject() {
        final String src = "[foo]";
        final Event e = subject(src, 1234);
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src)).isEqualTo(new BigDecimal("1234"));
    }

    @Test
    public void convertEventFieldToDoubleFromIncompatibleObject() {
        final String src = "[foo]";
        final Event e = subject(src, new HashMap());
        exception.expect(NumberFormatException.class);
        Converters.select("float").convert(e, src);
    }

    @Test
    public void convertEventFieldToIntFromNull() {
        final String src = "[foo]";
        final Event e = subject(src, 1234);
        exception.expect(NumberFormatException.class);
        Converters.select("int").convert(e, "[bar]");
    }

    @Test
    public void convertEventFieldToDoubleFromNull() {
        final String src = "[foo]";
        final Event e = subject(src, 1234);
        exception.expect(NumberFormatException.class);
        Converters.select("float").convert(e, "[bar]");
    }
}
