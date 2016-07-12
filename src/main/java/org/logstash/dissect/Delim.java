package org.logstash.dissect;

interface Delim {
    int indexOf(byte[] data, int offset);
    int size();
    String string();
}
