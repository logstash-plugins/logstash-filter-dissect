package org.logstash.dissect;

import org.logstash.Event;
import org.logstash.dissect.fields.Field;
import org.logstash.dissect.fields.FieldComparator;
import org.logstash.dissect.fields.FieldFactory;
import org.logstash.dissect.fields.NormalField;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dissector {
    public static final Field MISSING_FIELD = new NormalField("missing_field", "", Field.MISSING_ORDINAL_HIGHEST);
    private static final Pattern DELIMITER_FIELD_PATTERN = Pattern.compile("(.*?)%\\{(.*?)}");
    private final List<Field> fields = new ArrayList<>();
    // skip fields are not savable, so will be excluded from the saveableFields list
    // saveable fields are field + values that need to be set on the Event or HashMap
    private final List<Field> saveableFields = new ArrayList<>();
    private int initialOffset;
    private Field lastField = MISSING_FIELD;

    private Dissector() {
    }

    // the constructor is private so the create method must be used to get a Dissector instance
    // this is to ensure that handleMapping is called after construction.
    public static Dissector create(final String mapping) {
        final Dissector dissector = new Dissector();
        dissector.handleMapping(mapping);
        return dissector;
    }

    private void handleMapping(final String mapping) {
        if (mapping.isEmpty()) {
            throw new IllegalArgumentException("The mapping string cannot be empty");
        }

        // First we build up the associations of field name and the previous and next delimiters
        // then we create immutable field instances.
        final List<FieldDelimiterHolder> list = createFieldAssociations(mapping);
        // now create the fields for real
        createFieldList(list);
    }

    private List<FieldDelimiterHolder> createFieldAssociations(final String mapping) {
        final List<FieldDelimiterHolder> list = new ArrayList<>();
        // setup a regex pattern matcher for the dissection mapping string
        final Matcher m = DELIMITER_FIELD_PATTERN.matcher(mapping);
        for (int fieldIndex = 0; m.find(); fieldIndex++) {
            // any match has a delimiter in the first group
            // and a field in the second group
            final Delimiter delimiter = Delimiter.create(m.group(1));
            final FieldDelimiterHolder temp = new FieldDelimiterHolder(fieldIndex, m.group(2));
            // for this field point its 'previous' delimiter to this one
            temp.setPrevious(delimiter);
            if (list.isEmpty()) {
                if (delimiter.size() > 0) {
                    initialOffset = delimiter.size();
                    delimiter.setGreedy(true);
                }
            } else {
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

    private void createFieldList(final List<FieldDelimiterHolder> list) {
        // here we take the list of what was found and build real fields and add them to the lists
        for (final FieldDelimiterHolder holder : list) {
            final Field field = FieldFactory.create(holder.getId(), holder.getName(), holder.getPrevious(), holder.getNext());
            fields.add(field);
            if (field.saveable()) {
                saveableFields.add(field);
            }
        }

        // the saveableFields List is fields List minus the Skip fields
        // sorted so AppendFields are last
        Collections.sort(saveableFields, new FieldComparator());
    }

    public int dissect(final byte[] source, final Map<String, Object> keyValueMap) {
        if (source.length == 0) {
            return -1;
        }
        // keyValueMap is a Map we get given - its is what we are updating with the keys and found values
         ValueRef[] valueRefs = createValueRefArray();
        // here we take the bytes (from a Ruby String), use the fields list to find each delimiter and
        // record the indexes where we find each fields value in the valueRefs
        // we use the integer id of the Field as the index to store the ValueRef in.
        // note: we have not extracted any strings from the source bytes yet.
        final int pos = dissectValues(source, valueRefs);
        // dissectValues returns the position the last delimiter was found
        ValueResolver resolver = new ValueResolver(source, valueRefs);
        // iterate through the sorted saveable fields only
        for (final Field field : saveableFields) {
            // allow the field to append its key and
            // use the resolver to extract the value from the source bytes
            field.append(keyValueMap, resolver);
        }
        valueRefs = null;
        resolver = null;
        return pos;
    }

    public int dissect(final byte[] source, final Event event) {
        if (source.length == 0) {
            return -1;
        }
        ValueRef[] valueRefs = createValueRefArray();

        final int pos = dissectValues(source, valueRefs);
        ValueResolver resolver = new ValueResolver(source, valueRefs);

        for (final Field field : saveableFields) {
            field.append(event, resolver);
        }
        valueRefs = null;
        resolver = null;
        return pos;
    }

    private ValueRef[] createValueRefArray() {
        final ValueRef[] array = new ValueRef[fields.size()];
        for(Field field : fields) {
            array[field.id()] = new ValueRef(field.name());
        }
        return array;
    }

    private int dissectValues(final byte[] source, final ValueRef[] fieldValueRefs) {
        int left = initialOffset;
        int pos = initialOffset;
        int fieldStart;
        int fieldLength;
        Delimiter next;
        Delimiter prev;
        int numFields = fields.size();
        for (int i = 0, fieldsSize = numFields - 1; i < fieldsSize; i++) {
            Field field = fields.get(i);
            fieldValueRefs[field.id()].clear();
            // each delimiter is given a strategy that uses the indexOf method
            // to search in the source bytes for itself starting from
            // where we think next field might begin (left)
            prev = field.previousDelimiter();
            // the user indicated greedy with -> OR the first field had delimiter(s) before it
            boolean repeatedDelimiters = prev != null && prev.isGreedy();
            while (repeatedDelimiters) {
                pos = prev.indexOf(source, left);
                if (pos == left) { // we found a delimiter
                    left = pos + prev.size();
                } else {
                    repeatedDelimiters = false;
                }
            }
            fieldStart = left;
            next = field.nextDelimiter();
            fieldLength = 0;
            pos = next.indexOf(source, left);
            if (pos > 0) {
                // pos is at the next delimiter
                // we have found the end of the field
                fieldLength = pos - left;
                // set left to be the end of the delimiter
                // where we hope is the start index of the next field
                left = pos + next.size();
            }
            fieldValueRefs[field.id()].update(fieldStart, fieldLength);
        }
        lastField = fields.get(numFields - 1);
        fieldValueRefs[lastField.id()].clear();
        prev = lastField.previousDelimiter();
        // the user indicated greedy with -> OR the first field had delimiter(s) before it
        boolean repeatedDelimiters = prev != null && prev.isGreedy();
        while (repeatedDelimiters) {
            pos = prev.indexOf(source, left);
            if (pos == left) { // we found a delimiter
                left = pos + prev.size();
            } else {
                repeatedDelimiters = false;
            }
        }
        fieldStart = left;
        pos = source.length;
        fieldLength = pos - left;
        fieldValueRefs[lastField.id()].update(fieldStart, fieldLength);
        return pos;
    }
}
