package org.logstash.dissect;

import org.logstash.dissect.fields.Field;
import org.logstash.dissect.fields.NormalField;

import java.util.Map;

public class ValueResolver {
    private final byte[] source;
    private final Map<Field, ValueRef> values;

    ValueResolver(byte[] source, Map<Field, ValueRef> values) {
        this.source = source;
        this.values = values;
    }

    public String get(Field field) {
        return values.get(field).extract(source);
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
