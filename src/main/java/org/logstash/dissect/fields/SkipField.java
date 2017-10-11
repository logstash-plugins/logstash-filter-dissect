package org.logstash.dissect.fields;

import org.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class SkipField extends AbstractField {

    private SkipField(final int id, final String name, String suffix, final Delimiter previous, final Delimiter next) {
        super(id, name, suffix, SKIP_ORDINAL_LOWEST, previous, next);

    }

    public static Field create(final int id, final String name, String suffix, final Delimiter previous, final Delimiter next) {
        return new SkipField(id, name, suffix, previous, next);
    }

    @Override
    public void append(final Event event, final ValueResolver values) {
    }

    @Override
    public void append(final Map<String, Object> keyValueMap, final ValueResolver values) {
    }

    @Override
    public boolean saveable() {
        return false;
    }

    @Override
    public String toString() {
        return buildToString(this.getClass().getName());
    }
}
