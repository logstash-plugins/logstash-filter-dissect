package org.logstash.dissect;

import com.logstash.Event;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class Dissector {
    private final DelimBuilder delimBuilder = new DelimBuilder();
    private final FieldBuilder fieldBuilder = new FieldBuilder();
    private final Pattern pattern = Pattern.compile("(.*?)%\\{(.*?)\\}");
    private String mapping;
    private ArrayList<IDelim> delimiters = new ArrayList<>();
    private ArrayList<IField> fields = new ArrayList<>();
    private ArrayList<IField> saveableFields = new ArrayList<>();
    private HashMap<IField, ValueRef> intermediates;
    private int initialOffset = 0;
    private IField lastField;

    public Dissector(String mapping) {
        this.mapping = mapping;
        parseMapping();
    }

    public int dissect(byte[] source, Map<String, Object> map) {
        int pos = dissectValues(source);
        ValueResolver resolver = new ValueResolver(source, intermediates);

        for (IField field : saveableFields) {
            field.append(map, resolver);
        }
        return pos;
    }

    public int dissect(byte[] source, Event event) {
        int pos = dissectValues(source);
        ValueResolver resolver = new ValueResolver(source, intermediates);

        for (IField field : saveableFields) {
            field.append(event, resolver);
        }
        return pos;
    }

    public int dissectWithError() throws Exception {
        return 1 / 0 ;
    }

    private int dissectValues(byte[] source) {
        if (source.length == 0) return 0;
        int left = initialOffset;
        int pos = initialOffset;
        ValueRef vr;
        for (IField field : fields) {
            IDelim delim = field.delimiter();
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
                intermediates.get(field).set(left, pos - left);
                left = pos + delim.size();
            }
        }
        left = pos + lastField.previousDelimSize();

        vr = intermediates.get(lastField);
        vr.set_position(left);
        vr.set_length(source.length - left);
        return pos;
    }

    private void parseMapping() {
        if (mapping.isEmpty()) return;

        Matcher m = pattern.matcher(mapping);
        while (m.find()) {
            handleFoundDelim(m.group(1));
            handleFoundField(m.group(2));
        }
        // normally f,d,f,d,f,d,f,d,f ds are one less than fs
        // but can be d,f,d,f,d,f,d,f - then drop first
        if (delimiters.size() == fields.size()) {
            // we don't need the first delim
            IDelim d = delimiters.remove(0);
            // but we do need to know where the first field starts
            initialOffset = d.size();
        }

        for (int i = 0; i < delimiters.size(); i++) {
            IDelim d = delimiters.get(i);
            IField f = fields.get(i);
            IField n = fields.get(i + 1);
            f.addNextDelim(d);
            n.addPreviousDelim(d);
        }

        delimiters.clear();

        intermediates = new HashMap<>(fields.size() + 1);

        for (IField field : fields) {
            intermediates.put(field, new ValueRef());
        }

        intermediates.put(Field.getMissing(), new ValueRef());

        int lastFieldIndex = fields.size() - 1;
        if (lastFieldIndex > -1) {
            lastField = fields.remove(lastFieldIndex);
        }

        // the fields List has the last element removed to allow for a
        // different way to set the value
        //the saveableFields List has only the Field and AppendField elements
        // sorted so AppendFields are last

        Collections.sort(saveableFields, new FieldComparator());
    }

    private void handleFoundField(String field) {
        IField f = fieldBuilder.build(field);
        fields.add(f);
        if (f.saveable()) {
            saveableFields.add(f);
        }
    }

    private void handleFoundDelim(String delim) {
        if (!delim.isEmpty()) {
            delimiters.add(delimBuilder.build(delim));
        }
    }

}
