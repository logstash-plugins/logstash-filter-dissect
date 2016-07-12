package org.logstash.dissect;

class FieldFactory {
    private static final String ERR_MSG = "NormalField cannot prefix with both Append and Indirect Prefix (%s): %s";

    public static Field create(String field) {
        if (field.isEmpty() || field.startsWith("?")) {
            return SkipField.create(field);
        }
        if (field.startsWith("+&")) {
            throw new InvalidFieldException(String.format(ERR_MSG, "+&", field));
        }
        if (field.startsWith("+")) {
            return AppendField.create(field);
        }
        if (field.startsWith("&+")) {
            throw new InvalidFieldException(String.format(ERR_MSG, "&+", field));
        }
        if (field.startsWith("&")) {
            return IndirectField.create(field);
        }
        return NormalField.create(field);
    }
}
