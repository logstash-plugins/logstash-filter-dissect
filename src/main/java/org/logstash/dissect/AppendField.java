package org.logstash.dissect;

import com.logstash.Event;

import java.util.Map;

class AppendField extends NormalField {
    private static final int ORD = 2;

    private AppendField(String s, int ord) {
        super(s, ord);
    }

    public static Field create(String s) {
        if (hasOrdinal(s)) {
            String[] parts = leftChop(s).split("/");
            return new AppendField(parts[0], ORD + Integer.valueOf(parts[1]));
        } else {
            return new AppendField(leftChop(s), ORD);
        }
    }

    private static boolean hasOrdinal(String s) {
        return s.contains("/");
    }

    @Override
    public void append(Event event, ValueResolver values) {
        if (event.includes(_name)) {
            Object old = event.getField(_name);
            event.setField(_name, old.toString() + joinString() + values.get(this));
        } else {
            super.append(event, values);
        }
    }

    @Override
    public void append(Map<String, Object> map, ValueResolver values) {
        if (map.containsKey(_name)) {
            Object old = map.get(_name);
            map.put(_name, old.toString() + joinString() + values.get(this));
        } else {
            super.append(map, values);
        }
    }
}
