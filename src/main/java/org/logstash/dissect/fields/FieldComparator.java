package org.logstash.dissect.fields;

import java.util.Comparator;

public final class FieldComparator implements Comparator<Field> {
    @Override
    public int compare(Field o1, Field o2) {
        return o1.ordinal() - o2.ordinal();
    }
}
