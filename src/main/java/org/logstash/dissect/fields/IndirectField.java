package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class IndirectField extends AbstractField {

    public static Field create(String s) {
        return new IndirectField(s);
    }

    private IndirectField(String s) {
        super(s, 100);
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
}
