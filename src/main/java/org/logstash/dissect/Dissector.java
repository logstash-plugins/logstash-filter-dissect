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
    // skip fields are not savable, so will be excluded from the saveableFields list
    // saveable fields are field + values that need to be set on the Event or HashMap
    private final List<Field> saveableFields = new ArrayList<>();
    private int initialOffset = 0;
    private Field lastField = NormalField.MISSING;

    private Dissector() {
    }
    // the constructor is private so the create method must be used to get a Dissector instance
    // this is to ensure that handleMapping is called after construction.
    public static Dissector create(String mapping) {
        Dissector dissector = new Dissector();
        dissector.handleMapping(mapping);
        return dissector;
    }

    private void handleMapping(String mapping) {
        if (mapping.isEmpty()) {
            throw new IllegalArgumentException("The mapping string cannot be empty");
        }

        // First we build up the associations of field name and the previous and next delimiters
        // then we create immutable field instances.
        List<FieldDelimiterHolder> list = createFieldAssociations(mapping);

        // the list has the last element removed to allow for
        // assigning the rest of the string to the last field
        createLastField(list);

        // now create the fields for real
        createFieldList(list);
    }

    private List<FieldDelimiterHolder> createFieldAssociations(String mapping) {
        ArrayList<FieldDelimiterHolder> list = new ArrayList<>();
        // setup a regex pattern matcher for the dissection mapping string
        Matcher m = delimiterFieldPattern.matcher(mapping);

        for (int fieldIndex = 0; m.find(); fieldIndex++) {
            // any match has a delimiter in the first group
            // and a field in the second group
            Delimiter delimiter = Delimiter.create(m.group(1));
            FieldDelimiterHolder temp = new FieldDelimiterHolder(m.group(2));
            if (list.isEmpty()) {
                if (delimiter.size() > 0) {
                    initialOffset = delimiter.size();
                }
            } else {
                // for this field point its 'previous' delimiter to this one
                temp.setPrevious(delimiter);
                // for the previous field point its 'next' delimiter to this one
                list.get(fieldIndex - 1).setNext(delimiter);
                // each field points to the delimiters that were before and after it
                // we need the next because it the one we are looking for as we scan
                // we use the previous when we append the found value to another fields value
                // the last field does not have a next delimiter.
            }
            list.add(fieldIndex, temp);
        }
        // this is not a list of fields yet, this is a list of what was found in the dissect mapping
        return list;
    }

    private void createLastField(List<FieldDelimiterHolder> list) {
        FieldDelimiterHolder last;
        // the fields List has the last element removed to allow for
        // the rest of the text to be assigned to the last field
        int lastFieldIndex = list.size() - 1;
        if ((list.size() - 1) > -1) {
            last = list.remove(lastFieldIndex);
            lastField = FieldFactory.create(last.name, last.previous, last.next);
            saveableFields.add(lastField);
        }
    }

    private void createFieldList(List<FieldDelimiterHolder> list) {
        // here we take the list of what was found and build real fields and add them to the lists
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
        // keValueMap is a map we get given - its is what we are updating with the keys and found values
        final Map<Field, ValueRef> fieldValueRefMap = createFieldValueRefMap();
        // here we take the bytes (from a Ruby String), use the fields list to find each delimiter and
        // record the indexes where we find each fields value in the fieldValueRefMap
        // note: we have not extracted any strings from the source bytes yet.
        int pos = dissectValues(source, fieldValueRefMap);
        // dissectValues returns the position the last delimiter was found
        ValueResolver resolver = new ValueResolver(source, fieldValueRefMap);
        // iterate through the sorted saveable fields only
        for (Field field : saveableFields) {
            // allow the field to append its key and
            // use the resolver to extract the value from the source bytes
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

    // An IdentityHashMap is needed here because fields can have the same name and ordinal
    // but differ in the identities of next and previous Delimiter instances.
    // So we really compare by identity.
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
            // this is the delimiter after the field aka next
            Delimiter next = field.nextDelimiter();
            boolean found = false;
            // assume the field is not found by mapping the field to a 'missing' ValueRef
            // a ValueRef of (0, 0) has a length of 0 so it will not extract
            // a string from the source bytes
            fieldValueRefMap.put(field, new ValueRef(0, 0));
            Delimiter prev = field.previousDelimiter();
            boolean repeatedDelimiters = (prev != null);
            while (repeatedDelimiters) {
                pos = prev.indexOf(source, left);
                if (pos == left) { // we found a delimiter
                    left = pos + prev.size();
                } else {
                    repeatedDelimiters = false;
                }
            }
            while (!found) {
                // each delimiter is given a strategy that the indexOf method
                // uses to search in the source bytes for itself starting from
                // where we think next field might begin (left)
                pos = next.indexOf(source, left);
                if (pos == left) {
                    // we found a delimiter
                    left = pos + next.size();
                } else {
                    found = true;
                }
            }
            if (pos > 0) {
                // now we have found a field and pos is advanced to the next delimiter
                // remap the field to a proper ValueRef.
                fieldValueRefMap.put(field, new ValueRef(left, pos - left));
                // set left to be the end of the delimiter - the start index of the next field
                left = pos + next.size();
            }
        }
        boolean repeatedLastDelimiters = (pos > 0);
        Delimiter prev = lastField.previousDelimiter();
        while (repeatedLastDelimiters) {
            pos = prev.indexOf(source, left);
            if (pos == left) { // we found a delimiter
                left = pos + prev.size();
            } else {
                repeatedLastDelimiters = false;
            }
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
