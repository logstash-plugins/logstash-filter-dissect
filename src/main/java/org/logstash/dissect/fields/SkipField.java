package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class SkipField extends AbstractField {

    public static Field create(String name, Delimiter previous, Delimiter next) {
        return new SkipField(name, previous, next);
    }

    private SkipField(String s, Delimiter previous, Delimiter next) {
        super(s, SKIP_ORDINAL_LOWEST, previous, next);
    }

    @Override
    public void append(Event event, ValueResolver values) {}

    @Override
    public void append(Map<String, Object> keyValueMap, ValueResolver values) {}

    @Override
    public boolean saveable() {
        return false;
    }

    @Override
    public String toString() {
        return buildToString(this.getClass().getName());
    }
}
