package org.logstash.dissect;

interface IDelim {
    int indexOf(byte[] data, int offset);
    int size();
    String string();
}
