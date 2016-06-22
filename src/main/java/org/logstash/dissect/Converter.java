package org.logstash.dissect;

import com.logstash.Event;

public interface Converter {
    void convert(Event e, String src);
}
