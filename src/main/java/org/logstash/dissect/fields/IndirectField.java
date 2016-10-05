package org.logstash.dissect.fields;

import org.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class IndirectField extends AbstractField {

    private IndirectField(String s, Delimiter previous, Delimiter next) {
        super(s, INDIRECT_ORDINAL_HIGHER, previous, next);
    }

    public static Field create(String s, Delimiter previous, Delimiter next) {
        return new IndirectField(s, previous, next);
    }

    @Override
    public boolean saveable() {
        return true;
    }

    @Override
    public void append(Map<String, Object> keyValueMap, ValueResolver values) {
        String indirectName = anyValue(name(), keyValueMap, values);
        if (!indirectName.isEmpty()) {
            keyValueMap.put(indirectName, values.get(this));
        }
    }

    @Override
    public void append(Event event, ValueResolver values) {
        String indirectName = anyValue(name(), event, values);
        if (!indirectName.isEmpty()) {
            event.setField(indirectName, values.get(this));
        }
    }

    private String anyValue(String key, Event event, ValueResolver values) {
        if (event.includes(key)) {
            Object val = event.getField(key);
            return String.valueOf(val);
        }
        return values.get(values.find(key, this));
    }

    protected String anyValue(String key, Map<String, Object> map, ValueResolver values) {
        if (map.containsKey(key)) {
            Object val = map.get(key);
            return String.valueOf(val);
        }
        return values.get(values.find(key, this));
    }

    @Override
    public String toString() {
        return buildToString(this.getClass().getName());
    }
}
