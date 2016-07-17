package org.logstash.dissect.fields;

public final class FieldFactory {
    private static final String MIXED_PREFIXES = "Field cannot prefix with both Append and Indirect Prefix (%s): %s";
    private static final String PREFIXED_EMPTY = "Field cannot be a prefix on its own without further text";

    public static Field create(String field) {
        if (field.isEmpty() || field.startsWith("?")) {
            return SkipField.create(removeLeadingCharIfPresent(field));
        }
        if (field.startsWith("+&")) {
            throw new InvalidFieldException(String.format(MIXED_PREFIXES, "+&", field));
        }
        if (field.startsWith("+")) {
            String shorterField = removeLeadingCharIfPresent(field);
            if (shorterField.isEmpty()) {
                throw new InvalidFieldException(PREFIXED_EMPTY);
            }
            return AppendField.create(shorterField);
        }
        if (field.startsWith("&+")) {
            throw new InvalidFieldException(String.format(MIXED_PREFIXES, "&+", field));
        }
        if (field.startsWith("&")) {
            String shorterField = removeLeadingCharIfPresent(field);
            if (shorterField.isEmpty()) {
                throw new InvalidFieldException(PREFIXED_EMPTY);
            }
            return IndirectField.create(shorterField);
        }
        return NormalField.create(field);
    }

    private static String removeLeadingCharIfPresent(String fieldName) {
        return fieldName.isEmpty() ? fieldName : fieldName.substring(1);
    }
}
