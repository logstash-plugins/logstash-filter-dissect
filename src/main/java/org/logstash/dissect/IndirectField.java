package org.logstash.dissect;

import com.logstash.Event;

import java.util.Map;

class IndirectField extends Field {

    public static IField createField(String s) {
        return new IndirectField(s);
    }

    public IndirectField(String s) {
        super(leftChop(s), 100);
    }

    @Override
    public void append(Event event, ValueResolver values) {
        String indirectName = anyValue(name(), event, values);
        if (!indirectName.isEmpty()) {
            event.setField(indirectName, values.get(this));
        }
    }

    @Override
    public void append(Map<String, Object> map, ValueResolver values) {
        String indirectName = anyValue(name(), map, values);
        if (!indirectName.isEmpty()) {
            map.put(indirectName, values.get(this));
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
