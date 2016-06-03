package org.logstash.dissect;

class DelimBuilder {
    public IDelim build(String delim) {
        if (delim.length() == 1) {
            return new DelimOne(delim);
        } else if (delim.length() == 2) {
            return new DelimTwo(delim);
        } else {
            return new DelimMany(delim);
        }
    }
}
