package org.logstash.dissect;

import java.io.Serializable;

final class ConvertPair implements Serializable {
    private static final long serialVersionUID = 7227865769253897223L;

    static final ConvertPair[] EMPTY_ARRAY = new ConvertPair[0];
    private final String lhs;
    private final String rhs;

    ConvertPair(final String left, final String right) {
        lhs = left;
        rhs = right;
    }

    String src() {
        return lhs;
    }

    String type() {
        return rhs;
    }
}
