package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class SkipField extends AbstractField {

    public static Field create(String s) {
        return new SkipField(s);
    }

    private SkipField(String s) {
        super(s, 0);
    }

    @Override
    public void append(Event event, ValueResolver values) {}

    @Override
    public void append(Map<String, Object> keyValueMap, ValueResolver values) {}

    @Override
    public boolean saveable() {
        return false;
    }
}
