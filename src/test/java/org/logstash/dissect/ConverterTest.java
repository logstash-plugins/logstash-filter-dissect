package org.logstash.dissect;

import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.logstash.Event;

import java.util.HashMap;

import static org.assertj.core.api.Assertions.*;

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
        Object expected = 1234L;
        if (actual instanceof Integer) {
            expected = 1234;
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void convertEventFieldToIntFromDoubleString() {
        final String src = "[foo]";
        final Event e = subject(src, "12.34");
        Converters.select("int").convert(e, src);
        Object actual = e.getField(src);
        Object expected = 12L;
        if (actual instanceof Integer) {
            expected = 12;
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void convertEventFieldToIntFromDoubleObject() {
        final String src = "[foo]";
        final Event e = subject(src, 12.34);
        Converters.select("int").convert(e, src);
        Object actual = e.getField(src);
        Object expected = 12L;
        if (actual instanceof Integer) {
            expected = 12;
        }
        assertThat(actual).isEqualTo(expected);
    }

    @Test
    public void convertEventFieldToIntFromIncompatibleObject() {
        final String src = "[foo]";
        final Event e = subject(src, new HashMap());
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"{}\"");
        Converters.select("int").convert(e, src);
    }

    @Test
    public void convertEventFieldToDouble() {
        final String src = "[foo]";
        final Event e = subject(src, "12.34");
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src)).isEqualTo(12.34);
    }

    @Test
    public void convertEventFieldToDoubleFromIntString() {
        final String src = "[foo]";
        final Event e = subject(src, "1234");
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src)).isEqualTo(1234.0);
    }

    @Test
    public void convertEventFieldToDoubleFromIntObject() {
        final String src = "[foo]";
        final Event e = subject(src, 1234);
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src)).isEqualTo(1234.0);
    }

    @Test
    public void convertEventFieldToDoubleFromIncompatibleObject() {
        final String src = "[foo]";
        final Event e = subject(src, new HashMap());
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"{}\"");
        Converters.select("float").convert(e, src);
    }

    @Test
    public void convertEventFieldToIntFromNull() {
        final String src = "[foo]";
        final Event e = subject(src, 1234);
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"null\"");
        Converters.select("int").convert(e, "[bar]");
    }

    @Test
    public void convertEventFieldToDoubleFromNull() {
        final String src = "[foo]";
        final Event e = subject(src, 1234);
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"null\"");
        Converters.select("float").convert(e, "[bar]");
    }
}
