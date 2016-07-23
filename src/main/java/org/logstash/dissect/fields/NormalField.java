package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class NormalField extends AbstractField {

    public static final Field MISSING = new NormalField("missing_field", MISSING_ORDINAL_HIGHEST);

    private NormalField(String name, int ordinal) {
        super(name, ordinal);
    }

    private NormalField(String name, Delimiter previous, Delimiter next) {
        super(name, NORMAL_ORDINAL_LOWER, previous, next);
    }

    private NormalField(String name, int ordinal, Delimiter previous, Delimiter next) {
        super(name, ordinal, previous, next);
    }

    public static Field create(String name, Delimiter previous, Delimiter next) {
        return new NormalField(name, previous, next);
    }

    @Override
    public boolean saveable() {
        return true;
    }

    @Override
    public void append(Map<String, Object> keyValueMap, ValueResolver values) {
        keyValueMap.put(this.name(), values.get(this));
    }

    @Override
    public void append(Event event, ValueResolver values) {
        event.setField(this.name(), values.get(this));
    }

    @Override
    public String toString() {
        return buildToString(this.getClass().getName());
    }
}
