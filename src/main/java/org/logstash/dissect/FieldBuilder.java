package org.logstash.dissect;

class FieldBuilder {
    public static IField build(String field) {
        if (field.isEmpty() || field.startsWith("?")) {
            return SkipField.createField(field);
        }
        if (field.startsWith("+")) {
            return AppendField.createField(field);
        }
        if (field.startsWith("&")) {
            return IndirectField.createField(field);
        }
        return Field.createField(field);
    }
}
