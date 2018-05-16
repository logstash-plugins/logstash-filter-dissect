package org.logstash.dissect;

final class FieldDelimiterHolder {
    private final int id;
    private final String name;
    private Delimiter previous;
    private Delimiter next;

    FieldDelimiterHolder(final int id, final String name, Delimiter previous) {
        this.id = id;
        this.name = name;
        this.previous = previous;
        this.next = null;
    }

    public int getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public Delimiter getPrevious() {
        return previous;
    }

    public Delimiter getNext() {
        return next;
    }

    public void setPrevious(final Delimiter previous) {
        this.previous = previous;
    }

    public void setNext(final Delimiter next) {
        this.next = next;
    }
}
