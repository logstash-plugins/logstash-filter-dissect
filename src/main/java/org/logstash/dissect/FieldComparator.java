package org.logstash.dissect;

import java.util.Comparator;

public class FieldComparator implements Comparator<IField> {
    @Override
    public int compare(IField o1, IField o2) {
        return o1.ordinal() - o2.ordinal();
    }
}

