package org.logstash.dissect;

import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.RubyString;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.Serializable;

final class DissectPair implements Serializable {
    private static final long serialVersionUID = 8736122873297905474L;

    static final DissectPair[] EMPTY_ARRAY = new DissectPair[0];

    static DissectPair[] createArrayFromHash(final RubyHash hash) {
        if (hash.isNil()) {
            return EMPTY_ARRAY;
        }
        // a hash iterator that is independent of JRuby 1.7 and 9.0
        // this does not use unchecked casts
        // RubyHash inst.to_a() creates an array of arrays each having the key and value as elements
        final IRubyObject[] dissectPairs = hash.to_a().toJavaArray();
        final DissectPair[] pairs = new DissectPair[dissectPairs.length];
        for (int idx = 0; idx < dissectPairs.length; idx++) {
            pairs[idx] = create((RubyArray) dissectPairs[idx]);
        }
        return pairs;
    }

    private static DissectPair create(final RubyArray pair) {
        return new DissectPair(pair.first().asString(), pair.last().toString());
    }

    private final RubyString lhs;
    private final String jlhs;
    private final boolean empty;
    private final Dissector dissector;

    private DissectPair(final RubyString left, final String val) {
        lhs = left;
        jlhs = lhs.toString();
        empty = val.isEmpty();
        if (empty) {
            dissector = new Dissector();
        } else {
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
