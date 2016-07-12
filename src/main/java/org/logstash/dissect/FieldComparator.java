package org.logstash.dissect;

import java.util.Comparator;

public class FieldComparator implements Comparator<Field> {
    @Override
    public int compare(Field o1, Field o2) {
        return o1.ordinal() - o2.ordinal();
    }
}

