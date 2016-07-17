package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class AppendField extends AbstractField {
    private static final int ORD = 2;

    private AppendField(String s, int ord) {
        super(s, ord);
    }

    public static Field create(String s) {
        if (hasOrdinal(s)) {
            String[] parts = s.split("/");
            return new AppendField(parts[0], ORD + Integer.valueOf(parts[1]));
        } else {
            return new AppendField(s, ORD);
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
        if (keyValueMap.containsKey(this.name)) {
            Object old = keyValueMap.get(this.name);
            keyValueMap.put(this.name, old.toString() + joinString() + values.get(this));
        } else {
            keyValueMap.put(this.name, values.get(this));
        }
    }

    @Override
    public void append(Event event, ValueResolver values) {
        if (event.includes(this.name)) {
            Object old = event.getField(this.name);
            event.setField(this.name, old.toString() + joinString() + values.get(this));
        } else {
            event.setField(this.name, values.get(this));
        }
    }
}
