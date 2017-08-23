package org.logstash.dissect;

import org.logstash.Event;

interface Converter {
    void convert(Event e, String src);
    default boolean isInvalid() {
        return false;
    }
}
