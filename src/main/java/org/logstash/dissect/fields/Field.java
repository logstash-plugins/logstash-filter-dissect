package org.logstash.dissect.fields;

import com.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;

public interface Field {
    void append(Map<String, Object> map, ValueResolver values);
    void append(Event event, ValueResolver values);
    boolean saveable();
    int previousDelimiterSize();
    Delimiter delimiter();
    int ordinal();
    String name();
}
