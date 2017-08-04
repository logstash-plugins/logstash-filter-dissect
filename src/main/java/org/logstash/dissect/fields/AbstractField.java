package org.logstash.dissect.fields;

import org.logstash.dissect.Delimiter;

//import java.util.concurrent.atomic.AtomicInteger;

public abstract class AbstractField implements Field, Comparable<Field> {
    /*
        These ordinal constants establish the saveable field sort order.
        Skip fields are not saveable so should not be in the saveable fields list but
        they need an ordinal for construction - so SKIP_ORDINAL_LOWEST is used.
        The ordering groups field type together regardless of their parsed order.
        The order within the Normal and Indirect field groups is unimportant.
        In the Append field group order is important. Each Append fields ordinal
        is an offset off of APPEND_ORDINAL_BASE.
     */

//    private static final AtomicInteger idGenerator = new AtomicInteger(0);

    private final int ordinal;
    private final Delimiter previous;
    private final Delimiter next;
    private final String name;
    private final String suffix;
    private final Integer id;

    AbstractField(final String name, final String suffix, final int ord) {
        ordinal = ord;
        this.name = name;
        this.suffix = suffix;
        previous = null;
        next = null;
        id = 0;
    }

    AbstractField(final int id, final String name, final String suffix, final int ordinal, final Delimiter previous, final Delimiter next) {
        this.name = name;
        this.suffix = suffix;
        this.ordinal = ordinal;
        this.previous = previous;
        this.next = next;
        if (this.next != null) {
            this.next.setGreedy(this.suffix.contains(GREEDY_SUFFIX));
        }
        this.id = id;
    }

    @Override
    public int compareTo(Field o) {
        return Integer.compare(id, o.hashCode());
    }

    @Override
    public int hashCode() {
        return id;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof Field && hashCode() == obj.hashCode();
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public int id() {
        return id;
    }

    @Override
    public int ordinal() {
        return ordinal;
    }

    String joinString() {
        if (previous == null || previous.size() == 0) {
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

    String buildToString(final String className) {
        return className + '{' +
                "name=" + this.name() +
                ", ordinal=" + this.ordinal() +
                ", previous=" + this.previous +
                ", next=" + this.next +
                '}';
    }
}
