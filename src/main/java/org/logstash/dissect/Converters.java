package org.logstash.dissect;

import org.logstash.Event;

public enum Converters implements Converter {
    INT {
        @Override
        public void convert(final Event e, final String src) {
            final Object value = e.getField(src);
            final int v = new Double(String.valueOf(value)).intValue();
            e.setField(src, v);
        }
    },
    FLOAT {
        @Override
        public void convert(final Event e, final String src) {
            final Object value = e.getField(src);
            final double v = Double.parseDouble(String.valueOf(value));
            e.setField(src, v);
        }
    };

    public static Converters select(final String toType) {
        return valueOf(toType.toUpperCase());
    }
}
