package org.logstash.dissect;

import java.util.Map;

public class ValueResolver {
    byte[] source;
    Map<Field, ValueRef> values;

    public ValueResolver(byte[] source, Map<Field, ValueRef> values) {
        this.source = source;
        this.values = values;
    }

    public String get(Field field) {
        return values.get(field).string(source);
    }

    public Field find(String name, Field notThis) {
        for(Field f : values.keySet()) {
            if(!f.equals(notThis) && name.equals(f.name())) {
                return f;
            }
        }
        return NormalField.getMissing();
    }
}
