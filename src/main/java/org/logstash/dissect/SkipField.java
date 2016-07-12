package org.logstash.dissect;

import com.logstash.Event;

import java.util.Map;

class SkipField extends NormalField {

    public static Field create(String s) {
        if (s.isEmpty()) {
            return new SkipField("");
        }
        if (s.length() > 1) {
            return new SkipField(leftChop(s));
        }
        return new SkipField(s);
    }

    public SkipField(String s) {
        super(s, 0);
    }

    @Override
    public void append(Event event, ValueResolver values) {}

    @Override
    public void append(Map<String, Object> map, ValueResolver values) {}

    @Override
    public boolean saveable() {
        return false;
    }
}
