package org.logstash.dissect;

import org.jruby.RubyString;

import java.io.Serializable;

final class DissectPair implements Serializable {
    private static final long serialVersionUID = 8736122873297905474L;

    static final DissectPair[] EMPTY_ARRAY = new DissectPair[0];
    private final RubyString lhs;
    private final String jlhs;
    private final boolean empty;
    private Dissector dissector;

    DissectPair(final RubyString left, final String val) {
        lhs = left;
        jlhs = lhs.toString();
        empty = val.isEmpty();
        if (!empty) {
            dissector = Dissector.create(val);
        }
    }

    RubyString key() {
        return lhs;
    }

    String javaKey() {
        return jlhs;
    }

    Dissector dissector() {
        return dissector;
    }

    boolean isEmpty() {
        return empty;
    }
}
