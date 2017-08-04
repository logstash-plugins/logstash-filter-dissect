package org.logstash.dissect.fields;

import org.logstash.Event;
import org.logstash.dissect.Delimiter;
import org.logstash.dissect.ValueResolver;

import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public interface Field {
    int SKIP_ORDINAL_LOWEST = 0;
    int NORMAL_ORDINAL_LOWER = 1;
    int APPEND_ORDINAL_BASE = 100;
    int INDIRECT_ORDINAL_HIGHER = 1000;
    int MISSING_ORDINAL_HIGHEST = 100000;
    Pattern SUFFIX_REGEX = Pattern.compile("(.+?)(/\\d{1,2}|->|/\\d{1,2}->|->/\\d{1,2})?$");
    Pattern ORDINAL_REGEX = Pattern.compile("\\d+");
    String GREEDY_SUFFIX = "->";

    static String[] extractNameSuffix(final String s) {
        final String[] result = {s, ""};
        Matcher m = SUFFIX_REGEX.matcher(s);
        if (m.matches()) {
            result[0] = m.group(1);
            result[1] = m.group(2) == null ? "" : m.group(2);
        }
        return result;
    }

    void append(Map<String, Object> map, ValueResolver values);

    void append(Event event, ValueResolver values);

    boolean saveable();

    Delimiter nextDelimiter();

    Delimiter previousDelimiter();

    int ordinal();

    String name();

    int id();
}
