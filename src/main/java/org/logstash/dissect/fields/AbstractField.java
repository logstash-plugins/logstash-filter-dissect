package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public abstract class AbstractField implements Field {
    private final int ordinal;
    protected final String name;

    protected Delimiter join;
    protected Delimiter next;

    protected AbstractField(String s, int ord) {
        name = s;
        ordinal = ord;
    }

    @Override
    public abstract boolean saveable();

    @Override
    public abstract void append(Map<String, Object> map, ValueResolver values);

    @Override
    public abstract void append(Event event, ValueResolver values);

    @Override
    public void addPreviousDelimiter(Delimiter d) {
        join = d;
    }

    @Override
    public void addNextDelimiter(Delimiter d) {
        next = d;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int ordinal() {
        return ordinal;
    }

    @Override
    public int previousDelimiterSize() {
        if (join == null) {
            return 0;
        }
        return join.size();
    }

    public String joinString() {
        if (join == null) {
            return " ";
        }
        return join.delimiterString();
    }

    @Override
    public Delimiter delimiter() {
        return next;
    }
}
