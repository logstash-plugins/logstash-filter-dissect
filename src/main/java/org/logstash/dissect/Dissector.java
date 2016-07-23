package org.logstash.dissect;

import com.logstash.Event;
import org.logstash.dissect.fields.Field;
import org.logstash.dissect.fields.FieldComparator;
import org.logstash.dissect.fields.FieldFactory;
import org.logstash.dissect.fields.NormalField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dissector {
    private final Pattern delimiterFieldPattern = Pattern.compile("(.*?)%\\{(.*?)\\}");
    private final List<Field> fields = new ArrayList<>();
    private final List<Field> saveableFields = new ArrayList<>();
    private int initialOffset = 0;
    private Field lastField = NormalField.MISSING;

    private Dissector() {
    }

    public static Dissector create(String mapping) {
        Dissector dissector = new Dissector();
        dissector.handleMapping(mapping);
        return dissector;
    }

    private void handleMapping(String mapping) {
        if (mapping.isEmpty()) {
            throw new IllegalArgumentException("The mapping string cannot be empty");
        }
        Matcher m = delimiterFieldPattern.matcher(mapping);

        /*
            First we build up the associations of field name and the previous and next delimiters
            then we create immutable field instances.
         */
        List<FieldDelimiterHolder> list = createFieldAssociations(m);

        // the tempFieldList has the last element removed to allow for a
        // different way to set the value
        createLastField(list);

        // now create the fields for real
        createFieldList(list);
    }

    private List<FieldDelimiterHolder> createFieldAssociations(Matcher m) {
        ArrayList<FieldDelimiterHolder> list = new ArrayList<>();
        for (int fieldIndex = 0; m.find(); fieldIndex++) {
            Delimiter delimiter = Delimiter.create(m.group(1));
            FieldDelimiterHolder temp = new FieldDelimiterHolder(m.group(2));
            if (list.isEmpty()) {
                if (delimiter.size() > 0)
                    initialOffset = delimiter.size();
            } else {
                temp.setPrevious(delimiter);
                list.get(fieldIndex - 1).setNext(delimiter);
            }
            list.add(fieldIndex, temp);
        }
        return list;
    }

    private void createLastField(List<FieldDelimiterHolder> list) {
        FieldDelimiterHolder last;
        // the fields List has the last element removed to allow for a
        // different way to set the value
        int lastFieldIndex = list.size() - 1;
        if ((list.size() - 1) > -1) {
            last = list.remove(lastFieldIndex);
            lastField = FieldFactory.create(last.name, last.previous, last.next);
            saveableFields.add(lastField);
        }
    }

    private void createFieldList(List<FieldDelimiterHolder> list) {
        for (FieldDelimiterHolder holder : list) {
            Field field = FieldFactory.create(holder.name, holder.previous, holder.next);
            fields.add(field);
            if (field.saveable()) {
                saveableFields.add(field);
            }
        }

        // the saveableFields List is fields List minus the Skip fields
        // sorted so AppendFields are last
        Collections.sort(saveableFields, new FieldComparator());
    }

    public int dissect(byte[] source, Map<String, Object> keyValueMap) {
        final Map<Field, ValueRef> fieldValueRefMap = createFieldValueRefMap();
        int pos = dissectValues(source, fieldValueRefMap);
        ValueResolver resolver = new ValueResolver(source, fieldValueRefMap);

        for (Field field : saveableFields) {
            field.append(keyValueMap, resolver);
        }
        return pos;
    }

    public int dissect(byte[] source, Event event) {
        final Map<Field, ValueRef> fieldValueRefMap = createFieldValueRefMap();
        int pos = dissectValues(source, fieldValueRefMap);
        ValueResolver resolver = new ValueResolver(source, fieldValueRefMap);

        for (Field field : saveableFields) {
            field.append(event, resolver);
        }
        return pos;
    }

    /*
        An IdentityHashMap is needed here because fields can have the same name and ordinal
        but differ in the identities of next and previous Delimiter instances.
        So we really compare by identity.
        We could consider adding an instanceId int property when creating Fields and
        this would mean that one Field is equal to another Field if they have the same instanceId.
     */
    private Map<Field, ValueRef> createFieldValueRefMap() {
        Map<Field, ValueRef> map = new IdentityHashMap<>(fields.size() + 1);
        map.put(NormalField.MISSING, new ValueRef(0, 0));
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

    private static class FieldDelimiterHolder {
        private final String name;
        private Delimiter previous;
        private Delimiter next;

        public FieldDelimiterHolder(String name) {
            this.name = name;
        }

        public void setPrevious(Delimiter previous) {
            this.previous = previous;
        }

        public void setNext(Delimiter next) {
            this.next = next;
        }
    }
}
