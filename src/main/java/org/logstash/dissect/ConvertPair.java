package org.logstash.dissect;

import org.jruby.RubyArray;
import org.jruby.RubyHash;
import org.jruby.runtime.builtin.IRubyObject;

import java.io.Serializable;

final class ConvertPair implements Serializable {
    private static final long serialVersionUID = 7227865769253897223L;

    static final ConvertPair[] EMPTY_ARRAY = new ConvertPair[0];

    static ConvertPair[] createArrayFromHash(final RubyHash hash) {
        if (hash.isNil()) {
            return EMPTY_ARRAY;
        }
        // a hash iterator that is independent of JRuby 1.7 and 9.0 (Visitor vs VisitorWithState)
        // this does not use unchecked casts
        // RubyHash inst.to_a() creates an array of arrays each having the key and value as elements
        final IRubyObject[] convertPairs = hash.to_a().toJavaArray();
        final ConvertPair[] pairs = new ConvertPair[convertPairs.length];
        for (int idx = 0; idx < convertPairs.length; idx++) {
            pairs[idx] = create((RubyArray) convertPairs[idx]);
        }
        return pairs;
    }

    private static ConvertPair create(final RubyArray pair) {
        return new ConvertPair(pair.first().toString(), pair.last().toString());
    }

    private final String source;
    private final String _type;
    private final Converter convertion;

    private ConvertPair(final String left, final String right) {
        source = left;
        _type = right;
        convertion = Converters.select(_type);
    }

    String src() {
        return source;
    }

    String type() {
        return _type;
    }

    Converter converter() {
        return convertion;
    }
}
