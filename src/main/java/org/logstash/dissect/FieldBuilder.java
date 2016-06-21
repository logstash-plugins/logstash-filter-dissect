package org.logstash.dissect;

class FieldBuilder {
    private static String ERR_MSG = "Field cannot prefix with both Append and Indirect Prefix (%s): %s";

    public static IField build(String field) {
        if (field.isEmpty() || field.startsWith("?")) {
            return SkipField.createField(field);
        }
        if (field.startsWith("+&")) {
            throw new InvalidFieldException(String.format(ERR_MSG, "+&", field));
        }
        if (field.startsWith("+")) {
            return AppendField.createField(field);
        }
        if (field.startsWith("&+")) {
            throw new InvalidFieldException(String.format(ERR_MSG, "&+", field));
        }
        if (field.startsWith("&")) {
            return IndirectField.createField(field);
        }
        return Field.createField(field);
    }
}
