package org.logstash.dissect;

import org.logstash.Event;

import java.math.BigDecimal;

public enum Converters implements Converter {
    INT {
        @Override
        public void convert(final Event e, final String src) {
            final Object value = e.getField(src);
            e.setField(src, new BigDecimal(String.valueOf(value)).toBigInteger());
        }
    },
    FLOAT {
        @Override
        public void convert(final Event e, final String src) {
            final Object value = e.getField(src);
            e.setField(src, new BigDecimal(String.valueOf(value)));
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
