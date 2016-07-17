package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public final class NormalField extends AbstractField {

    private static final Field missing = new NormalField("missing field", 100000);
    public static Field getMissing() {
        return missing;
    }

    public static Field create(String s) {
        return new NormalField(s);
    }

    private NormalField(String s) {
        super(s, 1);
    }

    private NormalField(String s, int ord) {
        super(s, ord);
    }

    @Override
    public boolean saveable() {
        return true;
    }

    @Override
    public void append(Map<String, Object> keyValueMap, ValueResolver values) {
        keyValueMap.put(this.name, values.get(this));
    }

    @Override
    public void append(Event event, ValueResolver values) {
      event.setField(this.name, values.get(this));
    }

    @Override
    public String toString() {
        final StringBuilder sb = new StringBuilder("NormalField{");
        sb.append("name=").append(this.name());
        sb.append(", ordinal=").append(this.ordinal());
        sb.append(", join=").append(this.join);
        sb.append(", next=").append(this.next);
        sb.append('}');
        return sb.toString();
    }
}
