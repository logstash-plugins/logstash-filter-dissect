package org.logstash.dissect;

import org.logstash.Event;

import java.math.BigDecimal;
import java.math.RoundingMode;

public enum Converters implements Converter {
    INT {
        @Override
        public void convert(final Event e, final String src) {
            final Object value = e.getField(src);
            BigDecimal bd = new BigDecimal(String.valueOf(value)).setScale(12, RoundingMode.FLOOR);
            e.setField(src, bd.longValue());
        }
    },
    FLOAT {
        @Override
        public void convert(final Event e, final String src) {
            final Object value = e.getField(src);
            BigDecimal bd = new BigDecimal(String.valueOf(value)).setScale(12, RoundingMode.FLOOR);
            e.setField(src, bd.doubleValue());
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
