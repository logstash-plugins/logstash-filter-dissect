package org.logstash.dissect;

class DelimBuilder {
    public static IDelim build(String delim) {
        if (delim.length() == 1) {
            return new DelimOne(delim);
        } else if (delim.length() == 2) {
            return new DelimTwo(delim);
        } else if(delim.length() > 2) {
            return new DelimMany(delim);
        } else {
            return new DelimZero();
        }
    }
}
