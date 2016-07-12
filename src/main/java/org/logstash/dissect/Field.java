package org.logstash.dissect;

import com.logstash.Event;

import java.util.Map;

interface Field {
    void append(Map<String, Object> map, ValueResolver values);
    void append(Event event, ValueResolver values);
    boolean saveable();
    void addPreviousDelim(Delim d);
    void addNextDelim(Delim d);
    int previousDelimSize();
    Delim delimiter();
    int ordinal();
    String name();
}
