package org.logstash.dissect;

class DelimFactory {
    public static Delim create(String delim) {
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
