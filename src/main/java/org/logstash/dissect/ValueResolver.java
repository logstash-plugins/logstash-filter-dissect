package org.logstash.dissect;

import java.util.HashMap;

public class ValueResolver {
    byte[] source;
    HashMap<IField, ValueRef> values;

    public ValueResolver(byte[] source, HashMap<IField, ValueRef> values) {
        this.source = source;
        this.values = values;
    }

    public String get(IField field) {
        return values.get(field).value(source);
    }

    public IField find(String name, IField notThis) {
        for(IField f : values.keySet()) {
            if(!f.equals(notThis) && name.equals(f.name())) {
                return f;
            }
        }
        return Field.getMissing();
    }
}
