package org.logstash.dissect;

import org.logstash.dissect.search.DoubleByteSearchStrategy;
import org.logstash.dissect.search.MultiByteSearchStrategy;
import org.logstash.dissect.search.SingleByteSearchStrategy;
import org.logstash.dissect.search.ZeroByteSearchStrategy;

class DelimiterFactory {
    public static Delimiter create(String delim) {
        if (delim.length() == 1) {
            return new Delimiter(delim).addStrategy(new SingleByteSearchStrategy());
        } else if (delim.length() == 2) {
            return new Delimiter(delim).addStrategy(new DoubleByteSearchStrategy());
        } else if(delim.length() > 2) {
            return new Delimiter(delim).addStrategy(new MultiByteSearchStrategy());
        } else {
            return new Delimiter(delim).addStrategy(new ZeroByteSearchStrategy());
        }
    }
}
