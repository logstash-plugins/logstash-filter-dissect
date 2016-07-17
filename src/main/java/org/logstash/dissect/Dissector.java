package org.logstash.dissect;

import com.logstash.Event;
import org.logstash.dissect.fields.Field;
import org.logstash.dissect.fields.FieldComparator;
import org.logstash.dissect.fields.FieldFactory;
import org.logstash.dissect.fields.NormalField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dissector {
    private final Pattern delimiterFieldPattern = Pattern.compile("(.*?)%\\{(.*?)\\}");
    private final List<Field> fields = new ArrayList<>();
    private final List<Field> saveableFields = new ArrayList<>();
    private int initialOffset = 0;
    private Field lastField;

    public Dissector(String mapping) {
        if (mapping.isEmpty()) {
            throw new IllegalArgumentException("The mapping string cannot be empty");
        }

        Matcher m = delimiterFieldPattern.matcher(mapping);

        for (int i = 0; m.find(); i++) {
            Delimiter delimiter = DelimiterFactory.create(m.group(1));
            Field field = FieldFactory.create(m.group(2));
            if (fields.size() == 0) {
                if (delimiter.size() > 0)
                    initialOffset = delimiter.size();
            } else {
                field.addPreviousDelimiter(delimiter);
                fields.get(i - 1).addNextDelimiter(delimiter);
            }
            fields.add(i, field);
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

    public int dissect(byte[] source, Map<String, Object> keyValueMap) {
        ResolveValue resolveValue = resolve(source);

        for (Field field : saveableFields) {
            field.append(keyValueMap, resolveValue.resolver);
        }
        return resolveValue.position;
    }

    public int dissect(byte[] source, Event event) {
        ResolveValue resolveValue = resolve(source);

        for (Field field : saveableFields) {
            field.append(event, resolveValue.resolver);
        }
        return resolveValue.position;
    }

    private ResolveValue resolve(byte[] source) {
        final Map<Field, ValueRef> fieldValueRefMap = createFieldValueRefMap();
        int pos = dissectValues(source, fieldValueRefMap);
        ValueResolver resolver = new ValueResolver(source, fieldValueRefMap);
        return new ResolveValue(pos, resolver);
    }

    private class ResolveValue {
        private final int position;
        private final ValueResolver resolver;

        public ResolveValue(int position, ValueResolver resolver) {
            this.position = position;
            this.resolver = resolver;
        }
    }

    private Map<Field, ValueRef> createFieldValueRefMap() {
        Map<Field, ValueRef> map = new HashMap<>();
        map.put(NormalField.getMissing(), new ValueRef(0, 0));
        return map;
    }

    private int dissectValues(byte[] source, Map<Field, ValueRef> fieldValueRefMap) {
        if (source.length == 0) {
            return 0;
        }
        int left = initialOffset;
        int pos = initialOffset;

        for (Field field : fields) {
            Delimiter delim = field.delimiter();
            boolean found = false;
            fieldValueRefMap.put(field, new ValueRef(0, 0));
            while (!found) {
                pos = delim.indexOf(source, left);
                if (pos == left) {
                    left = pos + delim.size();
                } else {
                    found = true;
                }
            }
            if (pos > 0) {
                fieldValueRefMap.put(field, new ValueRef(left, pos - left));
                left = pos + delim.size();
            }
        }
        if (pos > 0) {
            left = pos + lastField.previousDelimiterSize();
        }
        ValueRef valueRef = new ValueRef(left, source.length - left);
        fieldValueRefMap.put(lastField, valueRef);
        return pos;
    }
}
