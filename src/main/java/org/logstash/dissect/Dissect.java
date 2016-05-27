package org.logstash.dissect;

import java.util.ArrayList;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dissect {
    private static final int HEAD = 0;
    private static final int MATCH = 1;
    private static final int TAIL = 2;
    private static final String CURLY_RE = "%\\{.*?\\}";
    private static final String QM = "?";
    private static final String BANG = "!";
    private static final String[] PARTS = {"", "", ""};
    private String source;
    private String mapping;
    private ArrayList<String> delimiters = new ArrayList<String>();
    private ArrayList<String> fields = new ArrayList<String>();
    private Pattern pattern;

    public Dissect(String source, String mapping) {
        this.source = source;
        this.mapping = mapping;
        pattern = Pattern.compile(CURLY_RE);
        parseMapping();
    }

    public void dissect(Map<String, Object> event) {
        if (source.isEmpty()) return;
        String[] parts = PARTS.clone();
        String tail;
        if (delimiters.size() == fields.size()) {
            String first = delimiters.remove(0);
            tail = source.substring(first.length());
        } else {
            tail = source;
        }
        for (int i = 0; i < delimiters.size(); i++) {
            String delim = delimiters.get(i);
            String field = fields.get(i);
            partitionStr(parts, tail, delim);
            tail = parts[TAIL];
            if (!QM.equals(field)) {
                append(event, field, parts[HEAD], i);
            }
        }
        int last = fields.size() - 1;
        append(event, fields.get(last), parts[TAIL], last);
    }

    private void append(Map<String, Object> event, String field, String val, int i) {
        if (field.endsWith(BANG)) {
            String name = field.substring(0, field.length() - 1);
            Object old = event.get(name);
            String value = old.toString() + delimiters.get(i - 1) + val;
            event.put(name, value);
        } else {
            event.put(field, val);
        }
    }

    private void parseMapping() {
        String tail = mapping;
        while (true) {
            if (tail.isEmpty()) break;
            String[] parts = PARTS.clone();
            partitionRex(parts, tail);
            if (!parts[HEAD].isEmpty()) {
                delimiters.add(parts[HEAD]);
            }
            fields.add(name(parts[MATCH]));
            tail = parts[TAIL];
        }
    }

    private String name(String part) {
        String n = part.substring(2, part.length() - 1);
        if (n.isEmpty()) {
            return QM;
        }
        return n;
    }

    private void partitionRex(String[] parts, String src) {
        Matcher m = pattern.matcher(src);
        if (m.find()) {
            if (m.start() != 0) {
                parts[0] = src.substring(0, m.start());
            }
            parts[1] = src.substring(m.start(), m.end());
            parts[2] = src.substring(m.end());
        }
    }

    private void partitionStr(String[] parts, String src, String delim) {
        String source = src;
        int start = source.indexOf(delim);

        // if the delimiter is at the head
        // move forward until we have start > 0
        while (start == 0) {
            source = source.substring(1);
            start = source.indexOf(delim);
        }
        int end = start + delim.length();
        if (start > 0) {
            parts[0] = source.substring(0, start);
            parts[1] = delim;
            parts[2] = source.substring(end);
        }
    }
}
