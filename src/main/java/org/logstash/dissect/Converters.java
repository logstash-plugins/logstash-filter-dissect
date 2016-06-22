package org.logstash.dissect;

import com.logstash.Event;

public enum Converters implements Converter{
    INT {
        public void convert(Event e, String src) {
            Object value = e.getField(src);
            int v = new Double(String.valueOf(value)).intValue();
            e.setField(src, v);
        }
    },
    FLOAT {
        public void convert(Event e, String src) {
            Object value = e.getField(src);
            double v = Double.parseDouble(String.valueOf(value));
            e.setField(src, v);
        }
    };

    public static Converters select(String toType) {
        return valueOf(toType.toUpperCase());
    }
}
