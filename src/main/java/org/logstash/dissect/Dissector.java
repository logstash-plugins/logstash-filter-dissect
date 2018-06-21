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
    private static final Pattern FINAL_DELIMITER_PATTERN = Pattern.compile("[^}]+$");
    private final List<Field> fields;
    // skip fields are not savable, so will be excluded from the saveableFields list
    // saveable fields are field + values that need to be set on the Event or HashMap
    private final List<Field> saveableFields;
    private int offsetOfFirstField;
    private String mapping;

    public  Dissector() {
        offsetOfFirstField = 0;
        fields = new ArrayList<>(10);
        saveableFields = new ArrayList<>(10);
        mapping = "";
    }

    public static Dissector create(final String mapping) {
        final Dissector dissector = new Dissector();
        dissector.handleMapping(mapping);
        return dissector;
    }

    public final String getMapping() {
        return mapping;
    }

    private void handleMapping(final String mapping) {
        this.mapping = mapping;
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
            // for this field point its 'previous' delimiter to this one
            final FieldDelimiterHolder temp = new FieldDelimiterHolder(fieldIndex, matcher.group(2), delimiter);
            if (list.isEmpty()) {
                if (delimiter.size() > 0) {
                    offsetOfFirstField = delimiter.size();
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
        final Matcher endsWithDelimiterMatcher = FINAL_DELIMITER_PATTERN.matcher(mapping);
        if (endsWithDelimiterMatcher.find()) {
            final Delimiter finalDelimiter = Delimiter.create(endsWithDelimiterMatcher.group(0));
            final FieldDelimiterHolder holder = new FieldDelimiterHolder(fieldIndex, "?auto_added_skip", finalDelimiter);
            list.add(fieldIndex, holder);
            list.get(fieldIndex - 1).setNext(finalDelimiter);
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

    public final DissectResult dissect(final byte[] source, final Map<String, Object> keyValueMap) {
        final ValueRef[] valueRefs = createValueRefArray();
        final DissectResult result = new DissectResult();
        if (fields.isEmpty() || source == null || source.length == 0) {
            result.bail();
        } else {
            // keyValueMap is a Map we get given - its is what we are updating with the keys and found values
            // here we take the bytes (from a Ruby String), use the fields list to find each delimiter and
            // record the indexes where we find each fields value in the valueRefs
            // we use the integer id of the Field as the index to store the ValueRef in.
            // note: we have not extracted any strings from the source bytes yet.
            dissectValues(source, valueRefs, result);
            final ValueResolver resolver = new ValueResolver(source, valueRefs);
            if (result.matched()) {
                // fields were found
                // fill the keyValueMap, iterate through the sorted saveable fields only
                for (final Field field : saveableFields) {
                    // allow the field to append its key and
                    // use the resolver to extract the value from the source bytes
                    field.append(keyValueMap, resolver);
                }
            }
        }
        return result;
    }

    public final DissectResult dissect(final byte[] source, final Event event) {
        final ValueRef[] valueRefs = createValueRefArray();
        final DissectResult result = new DissectResult();
        if (fields.isEmpty() || source.length == 0) {
            result.bail();
        } else {
            dissectValues(source, valueRefs, result);
            final ValueResolver resolver = new ValueResolver(source, valueRefs);
            if (result.matched()) {
                for (final Field field : saveableFields) {
                    field.append(event, resolver);
                }
            }
        }
        return result;
    }

    private ValueRef[] createValueRefArray() {
        final ValueRef[] array = new ValueRef[fields.size()];
        for(final Field field : fields) {
            array[field.id()] = new ValueRef(field.name());
        }
        return array;
    }

    private void dissectValues(final byte[] source, final ValueRef[] fieldValueRefs, final DissectResult result) {
        final Dissector.Position position = new Dissector.Position(source, offsetOfFirstField);
        final int numFields = fields.size();
        final int lastFieldIndex = numFields - 1;
        for (int idx = 0; idx < lastFieldIndex; idx++) {
            final Field field = fields.get(idx);
            fieldValueRefs[field.id()].clear();
            // each delimiter is given a strategy that uses the indexOf method
            // to search in the source bytes for itself starting from
            // where we think next field might begin (left)
            position.moveBeyondDelimiter(field.previousDelimiter(), result);
            if (result.notMatched()) {
                break;
            }
            position.moveNext(field.nextDelimiter(), result);
            if (result.notMatched()) {
                break;
            }
            fieldValueRefs[field.id()].update(position.start, position.length);
        }
        if (result.matched()) {
            final Field lastField = fields.get(lastFieldIndex);
            fieldValueRefs[lastField.id()].clear();
            position.moveBeyondDelimiter(lastField.previousDelimiter(), result);
            position.repositionToEnd();
            fieldValueRefs[lastField.id()].update(position.start, position.length);
        }
    }

    private static final class Position {
        int pos;
        final int firstFieldOffset;
        int left;
        final byte[] source;
        int start;
        int length;

        Position(final byte[] sourceBytes, final int offsetFirstField) {
            source = sourceBytes;
            firstFieldOffset = offsetFirstField;
            left = 0;
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

        void moveNext(final Delimiter next, final DissectResult result) {
            length = 0;
            pos = next.indexOf(source, left);
            if (pos == -1) {
                // the next delimiter was never found at all, bail out
                result.bail();
            } else if (pos > 0) {
                // pos is now at the next delimiter, found the end of the field
                setLength();
                // set left to be the end of the delimiter & start index of the next field
                left = pos + next.size();
            }
        }

        void moveBeyondDelimiter(final Delimiter prev, final DissectResult result) {
            // we use this method to move past one or more consecutive delimiters if greedy
            if (prev == null) {
                // we are at the start
                setStart();
            } else if (prev.isGreedy()) {
                // greedy consume, used '->' suffix
                int foundCount = 0;
                while (true) {
                    pos = prev.indexOf(source, left);
                    if (pos == left) {
                        // we found another delimiter move to the end of the delimiter
                        foundCount++;
                        left = pos + prev.size();
                    } else if (pos == -1) {
                        // the previous delimiter was not found at all in the rest of the value
                        // this is OK start from left
                        if (foundCount == 0) {
                            // we never found the delimiter at all
                            result.bail();
                        }
                        setStart();
                        break;
                    } else {
                        // we found the start of value
                        setStart();
                        break;
                    }
                }
            } else {
                // not greedy
                // need to handle the starting delimiter case
                if (left == 0 && firstFieldOffset > 0) {
                    // there is a first delimiter and we have not yet found it and skipped over it.
                    pos = prev.indexOf(source, left);
                    // first delimiter must appear just before the start of the value.
                    if (pos == 0) {
                        left = pos + prev.size();
                    } else {
                        // if -1, no first delimiter was found at all or
                        // > 0, the delimiter was found deeper in the value
                        // either way we bail by setting start to the end
                        result.bail();
                    }
                }
                // we move the start to be where the left is now.
                // left is at the start of the field (after this delimiter)
                setStart();
            }
        }
    }
}
