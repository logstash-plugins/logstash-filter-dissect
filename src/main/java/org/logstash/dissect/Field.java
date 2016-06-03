package org.logstash.dissect;

import com.logstash.Event;

import java.util.Map;

class Field implements IField {
    private int _ordinal;
    protected final String _name;

    protected IDelim _join;
    protected IDelim _next;

    private static final IField missing = new Field("missing field", 100000);

    public static IField createField(String s) {
        return new Field(s);
    }

    public static IField getMissing() {
        return missing;
    }

    public Field(String s) {
        this(s, 1);
    }

    protected Field(String s, int ord) {
        _name = s;
        _ordinal = ord;
    }

    protected static String leftChop(String s) {
        return s.substring(1);
    }

    public boolean saveable() {
        return true;
    }

    public void append(Map<String, Object> map, ValueResolver values) {
        map.put(_name, values.get(this));
    }

    public void append(Event event, ValueResolver values) {
      event.setField(_name, values.get(this));
    }

    public void addPreviousDelim(IDelim d) {
        _join = d;
    }

    public void addNextDelim(IDelim d) {
        _next = d;
    }

    @Override
    public String name() {
        return _name;
    }

    public int ordinal() {
        return _ordinal;
    }

    public int previousDelimSize() {
        if (_join == null) {
            return 0;
        }
        return _join.size();
    }

    public String joinString() {
        if (_join == null) {
            return " ";
        }
        return _join.string();
    }

    public IDelim delimiter() {
        return _next;
    }

    @Override
    public String toString() {
        return "'" + _name + "'";
    }
}
