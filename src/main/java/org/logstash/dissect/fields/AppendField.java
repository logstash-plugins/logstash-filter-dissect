package org.logstash.dissect.fields;

import org.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class AppendField extends AbstractField {

    private AppendField(String s, int ord, Delimiter previous, Delimiter next) {
        super(s, ord, previous, next);
    }

    public static Field create(String s, Delimiter previous, Delimiter next) {
        if (hasOrdinal(s)) {
            String[] parts = s.split("\\W|_");
            return new AppendField(parts[0], APPEND_ORDINAL_BASE + Integer.valueOf(parts[1]), previous, next);
        } else {
            return new AppendField(s, APPEND_ORDINAL_BASE, previous, next);
        }
    }

    private static boolean hasOrdinal(String s) {
        return s.contains("/");
    }

    @Override
    public boolean saveable() {
        return true;
    }

    @Override
    public void append(Map<String, Object> keyValueMap, ValueResolver values) {
        if (keyValueMap.containsKey(this.name())) {
            Object old = keyValueMap.get(this.name());
            keyValueMap.put(this.name(), old.toString() + joinString() + values.get(this));
        } else {
            keyValueMap.put(this.name(), values.get(this));
        }
    }

    @Override
    public void append(Event event, ValueResolver values) {
        if (event.includes(this.name())) {
            Object old = event.getField(this.name());
            event.setField(this.name(), old.toString() + joinString() + values.get(this));
        } else {
            event.setField(this.name(), values.get(this));
        }
    }

    @Override
    public String toString() {
        return buildToString(this.getClass().getName());
    }
}
