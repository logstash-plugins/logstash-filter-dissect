package org.logstash.dissect;

import com.logstash.Event;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;

import java.util.HashMap;

import static org.hamcrest.core.Is.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertThat;

public class ConverterTest {
    @Rule
    public ExpectedException exception = ExpectedException.none();

    private Event subject(String src, Object val) {
        Event e = new Event();
        e.setField(src, val);
        return e;
    }

    @Test
    public void convertEventFieldToInt() {
        String src = "[foo]";
        Event e = subject(src, "1234");
        Converters.select("int").convert(e, src);
        assertThat(e.getField(src), is(equalTo(1234)));
    }

    @Test
    public void convertEventFieldToIntFromDoubleString() {
        String src = "[foo]";
        Event e = subject(src, "12.34");
        Converters.select("int").convert(e, src);
        assertThat(e.getField(src), is(equalTo(12)));
    }

    @Test
    public void convertEventFieldToIntFromDoubleObject() {
        String src = "[foo]";
        Event e = subject(src, 12.34);
        Converters.select("int").convert(e, src);
        assertThat(e.getField(src), is(equalTo(12)));
    }

    @Test
    public void convertEventFieldToIntFromIncompatibleObject() {
        String src = "[foo]";
        Event e = subject(src, new HashMap());
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"{}\"");
        Converters.select("int").convert(e, src);
    }

    @Test
    public void convertEventFieldToDouble() {
        String src = "[foo]";
        Event e = subject(src, "12.34");
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src), is(equalTo(12.34)));
    }

    @Test
    public void convertEventFieldToDoubleFromIntString() {
        String src = "[foo]";
        Event e = subject(src, "1234");
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src), is(equalTo(1234.0)));
    }

    @Test
    public void convertEventFieldToDoubleFromIntObject() {
        String src = "[foo]";
        Event e = subject(src, 1234);
        Converters.select("float").convert(e, src);
        assertThat(e.getField(src), is(equalTo(1234.0)));
    }

    @Test
    public void convertEventFieldToDoubleFromIncompatibleObject() {
        String src = "[foo]";
        Event e = subject(src, new HashMap());
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"{}\"");
        Converters.select("float").convert(e, src);
    }

    @Test
    public void convertEventFieldToIntFromNull() {
        String src = "[foo]";
        Event e = subject(src, 1234);
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"null\"");
        Converters.select("int").convert(e, "[bar]");
    }

    @Test
    public void convertEventFieldToDoubleFromNull() {
        String src = "[foo]";
        Event e = subject(src, 1234);
        exception.expect(NumberFormatException.class);
        exception.expectMessage("For input string: \"null\"");
        Converters.select("float").convert(e, "[bar]");
    }
}
