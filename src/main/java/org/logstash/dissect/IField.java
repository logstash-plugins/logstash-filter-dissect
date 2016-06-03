package org.logstash.dissect;

import com.logstash.Event;

import java.util.Map;

interface IField {
    void append(Map<String, Object> map, ValueResolver values);
    void append(Event event, ValueResolver values);
    boolean saveable();
    void addPreviousDelim(IDelim d);
    void addNextDelim(IDelim d);
    int previousDelimSize();
    IDelim delimiter();
    int ordinal();
    String name();
}
