package org.logstash.dissect;

import com.logstash.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dissector {
    private final Pattern pattern = Pattern.compile("(.*?)%\\{(.*?)\\}");
    private final List<Field> fields = new ArrayList<>();
    private final List<Field> saveableFields = new ArrayList<>();
    private final Map<Field, ValueRef> fieldValueRefMap = new HashMap<>();
    private int initialOffset = 0;
    private Field lastField;

    public Dissector(String mapping) {
        if (mapping.isEmpty()) return;

        fieldValueRefMap.put(NormalField.getMissing(), new ValueRef());
        Matcher m = pattern.matcher(mapping);

        for (int i = 0; m.find(); i++) {
            Delim delim = DelimFactory.create(m.group(1));
            Field field = FieldFactory.create(m.group(2));
            if (fields.size() == 0) {
                if (!(delim instanceof DelimZero))
                    initialOffset = delim.size();
            } else {
                field.addPreviousDelim(delim);
                fields.get(i - 1).addNextDelim(delim);
            }
            fields.add(i, field);
            fieldValueRefMap.put(field, new ValueRef());
            if (field.saveable()) {
                saveableFields.add(field);
            }
        }

        // the fields List has the last element removed to allow for a
        // different way to set the value
        int lastFieldIndex = fields.size() - 1;
        if ((fields.size() - 1) > -1) {
            lastField = fields.remove(lastFieldIndex);
        }

        // the saveableFields List is fields List minus the Skip fields
        // sorted so AppendFields are last
        Collections.sort(saveableFields, new FieldComparator());
    }

    public int dissect(byte[] source, Map<String, Object> map) {
        int pos = dissectValues(source);
        ValueResolver resolver = new ValueResolver(source, fieldValueRefMap);

        for (Field field : saveableFields) {
            field.append(map, resolver);
        }
        return pos;
    }

    public int dissect(byte[] source, Event event) {
        int pos = dissectValues(source);
        ValueResolver resolver = new ValueResolver(source, fieldValueRefMap);

        for (Field field : saveableFields) {
            field.append(event, resolver);
        }
        return pos;
    }

    private int dissectValues(byte[] source) {
        if (source.length == 0) return 0;
        int left = initialOffset;
        int pos = initialOffset;

        for (Field field : fields) {
            Delim delim = field.delimiter();
            boolean found = false;
            while (!found) {
                pos = delim.indexOf(source, left);
                if (pos == left) {
                    left = pos + delim.size();
                } else {
                    found = true;
                }
            }
            if (pos > 0) {
                fieldValueRefMap.get(field).set(left, pos - left);
                left = pos + delim.size();
            }
        }
        left = pos + lastField.previousDelimSize();
        fieldValueRefMap.get(lastField).set(left, source.length - left);
        return pos;
    }
}
