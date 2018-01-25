package org.logstash.dissect;

import org.logstash.Event;
import org.logstash.dissect.fields.Field;
import org.logstash.dissect.fields.FieldComparator;
import org.logstash.dissect.fields.FieldFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dissector {
    private static final Pattern DELIMITER_FIELD_PATTERN = Pattern.compile("(.*?)%\\{([^}]*?)}", Pattern.DOTALL);
    private final List<Field> fields;
    // skip fields are not savable, so will be excluded from the saveableFields list
    // saveable fields are field + values that need to be set on the Event or HashMap
    private final List<Field> saveableFields;
    private int initialOffset;

    public  Dissector() {
        initialOffset = 0;
        fields = new ArrayList<>(10);
        saveableFields = new ArrayList<>(10);
    }

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
        final List<FieldDelimiterHolder> list = new ArrayList<>(10);
        // setup a regex pattern matcher for the dissection mapping string
        final Matcher matcher = DELIMITER_FIELD_PATTERN.matcher(mapping);
        int fieldIndex = 0;
        while (matcher.find()) {
            // any match has a delimiter in the first group
            // and a field in the second group
            final Delimiter delimiter = Delimiter.create(matcher.group(1));
            final FieldDelimiterHolder temp = new FieldDelimiterHolder(fieldIndex, matcher.group(2));
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
            fieldIndex++;
        }
        // this is not a list of fields yet, this is a list of what was found in the dissect mapping
        return list;
    }

    private void createFieldList(final Iterable<FieldDelimiterHolder> list) {
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
        saveableFields.sort(new FieldComparator());
    }

    public final int dissect(final byte[] source, final Map<String, Object> keyValueMap) {
        final int pos;
        if (fields.isEmpty() || source == null || source.length == 0) {
            pos = -1;
        } else {
            // keyValueMap is a Map we get given - its is what we are updating with the keys and found values
            final ValueRef[] valueRefs = createValueRefArray();
            // here we take the bytes (from a Ruby String), use the fields list to find each delimiter and
            // record the indexes where we find each fields value in the valueRefs
            // we use the integer id of the Field as the index to store the ValueRef in.
            // note: we have not extracted any strings from the source bytes yet.
            pos = dissectValues(source, valueRefs);
            // dissectValues returns the position the last delimiter was found
            final ValueResolver resolver = new ValueResolver(source, valueRefs);
            // iterate through the sorted saveable fields only
            for (final Field field : saveableFields) {
                // allow the field to append its key and
                // use the resolver to extract the value from the source bytes
                field.append(keyValueMap, resolver);
            }

        }
        return pos;
    }

    public final int dissect(final byte[] source, final Event event) {
        final int pos;
        if (fields.isEmpty() || source.length == 0) {
            pos = -1;
        } else {
            final ValueRef[] valueRefs = createValueRefArray();

            pos = dissectValues(source, valueRefs);
            final ValueResolver resolver = new ValueResolver(source, valueRefs);

            for (final Field field : saveableFields) {
                field.append(event, resolver);
            }
        }
        return pos;
    }

    private ValueRef[] createValueRefArray() {
        final ValueRef[] array = new ValueRef[fields.size()];
        for(final Field field : fields) {
            array[field.id()] = new ValueRef(field.name());
        }
        return array;
    }

    private int dissectValues(final byte[] source, final ValueRef[] fieldValueRefs) {
        final Dissector.Position position = new Dissector.Position(source, initialOffset);

        final int numFields = fields.size();
        final int lastFieldIndex = numFields - 1;
        for (int idx = 0; idx < lastFieldIndex; idx++) {
            final Field field = fields.get(idx);
            fieldValueRefs[field.id()].clear();
            // each delimiter is given a strategy that uses the indexOf method
            // to search in the source bytes for itself starting from
            // where we think next field might begin (left)
            position.moveBeyondDelimiter(field.previousDelimiter());
            position.moveNext(field.nextDelimiter());
            fieldValueRefs[field.id()].update(position.start, position.length);
        }
        final Field lastField = fields.get(lastFieldIndex);
        fieldValueRefs[lastField.id()].clear();
        position.moveBeyondDelimiter(lastField.previousDelimiter());
        position.repositionToEnd();
        fieldValueRefs[lastField.id()].update(position.start, position.length);
        return position.pos;
    }

    private static final class Position {
        int pos;
        int left;
        final byte[] source;
        int start;
        int length;

        Position(final byte[] sourceBytes, final int initial) {
            source = sourceBytes;
            left = initial;
            pos = 0;
            start = 0;
            length = 0;
        }

        void setStart() {
            start = left;
        }

        void setLength() {
            length = pos - left;
        }

        void repositionToEnd() {
            pos = source.length;
            setLength();
        }

        void moveNext(final Delimiter next) {
            length = 0;
            pos = next.indexOf(source, left);
            if (pos > 0) {
                // pos is now at the next delimiter, found the end of the field
                setLength();
                // set left to be the end of the delimiter & start index of the next field
                left = pos + next.size();
            }
        }

        void moveBeyondDelimiter(final Delimiter prev) {
            if (prev == null || !prev.isGreedy()) {
                // no delimiter or not greedy (did not use '->')
                // we are at the start
                setStart();
            } else {
                // greedy consume,
                while (true) {
                    pos = prev.indexOf(source, left);
                    if (pos != left) {
                        // we found the start of value
                        setStart();
                        break;
                    }
                    // we found a delimiter move to the end of the delimiter
                    left = pos + prev.size();
                }
            }
        }
    }
}
