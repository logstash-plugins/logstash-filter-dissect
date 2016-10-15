package org.logstash.dissect;

import org.logstash.Event;

public interface Converter {
    void convert(Event e, String src);
}
