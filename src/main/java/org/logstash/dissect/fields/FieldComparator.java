package org.logstash.dissect.fields;

import java.util.Comparator;

public final class FieldComparator implements Comparator<Field> {
    @Override
    public int compare(final Field o1, final Field o2) {
        return o1.ordinal() - o2.ordinal();
    }
}
