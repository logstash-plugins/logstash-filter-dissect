package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public abstract class AbstractField implements Field {
    /*
        These ordinal constants establish the saveable field sort order.
        Skip fields are not saveable so should not be in the saveable fields list but
        they need an ordinal for construction - so SKIP_ORDINAL_LOWEST is used.
        The ordering groups field type together regardless of their parsed order.
        The order within the Normal and Indirect field groups is unimportant.
        In the Append field group order is important. Each Append fields ordinal
        is an offset off of APPEND_ORDINAL_BASE.
     */
    public static final int SKIP_ORDINAL_LOWEST = 0;
    public static final int NORMAL_ORDINAL_LOWER = 1;
    public static final int APPEND_ORDINAL_BASE = 100;
    public static final int INDIRECT_ORDINAL_HIGHER = 1000;
    public static final int MISSING_ORDINAL_HIGHEST = 100000;

    private final int ordinal;
    private final String name;

    private final Delimiter previous;
    private final Delimiter next;

    protected AbstractField(String s, int ord) {
        name = s;
        ordinal = ord;
        previous = null;
        next = null;
    }

    public AbstractField(String name, int ordinal, Delimiter previous, Delimiter next) {
        this.ordinal = ordinal;
        this.name = name;
        this.previous = previous;
        this.next = next;
    }

    @Override
    public abstract boolean saveable();

    @Override
    public abstract void append(Map<String, Object> map, ValueResolver values);

    @Override
    public abstract void append(Event event, ValueResolver values);

    @Override
    public String name() {
        return name;
    }

    @Override
    public int ordinal() {
        return ordinal;
    }

    public String joinString() {
        if (previous == null) {
            return " ";
        }
        return previous.getDelimiter();
    }

    @Override
    public Delimiter nextDelimiter() {
        return next;
    }

    @Override
    public Delimiter previousDelimiter() {
        return previous;
    }

    protected String buildToString(String className) {
        final StringBuilder sb = new StringBuilder(className);
        sb.append("{");
        sb.append("name=").append(this.name());
        sb.append(", ordinal=").append(this.ordinal());
        sb.append(", previous=").append(this.previous);
        sb.append(", next=").append(this.next);
        sb.append('}');
        return sb.toString();
    }
}
