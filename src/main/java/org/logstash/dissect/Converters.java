package org.logstash.dissect;

import org.logstash.Event;

public enum Converters implements Converter {
    INT {
        @Override
        public void convert(final Event e, final String src) {
            final Object value = e.getField(src);
            final long v = new Double(String.valueOf(value)).longValue();
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
    },
    NULL_CONVERTER {
        @Override
        public void convert(final Event e, final String src) {}

        @Override
        public boolean isInvalid() {
            return true;
        }
    };

    public static Converters select(final String toType) {
        Converters result = NULL_CONVERTER;
        try {
            result = valueOf(toType.toUpperCase());
        } catch (final IllegalArgumentException e) {
            // do nothing
        }
        return result;
    }
}
