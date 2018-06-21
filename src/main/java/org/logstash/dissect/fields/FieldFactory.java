package org.logstash.dissect.fields;

import org.logstash.dissect.Delimiter;

/*
    Normal field notation
    The found value is added to the Event using the key.
    `%{some_field}` - a normal field

    Skip field notation
    The found value is recorded internally but not added to the Event.
    The key, if supplied, is prefixed with a `?`.
    `%{}` - an empty skip field
    `%{?some_field} - named skip field

    Append field notation
    The value is appended to another value or stored if its the first field seen.
    The key is prefixed with a `+`.
    The final value is stored in the Event using the key.
    The delimiter found before the field or a space is appended before the found value.
    `%{+some_field}` - an append field
    `%{+some_field/2}` - and append field with an order modifier.
    An order modifier, `/number`, allows one to reorder the append sequence.
    e.g. for a text of `1 2 3 go`, this `%{+a/2} %{+a/1} %{+a/4} %{+a/3}` will build a key/value of `a => 2 1 go 3`
    Append fields without an order modifier will append in declared order.
    e.g. for a text of `1 2 3 go`, this `%{a} %{b} %{+a}` will build two key/values of `a => 1 3 go, b => 2`

    Indirect field notation
    The found value is added to the Event using the found value of another field as the key.
    The key is prefixed with a `&`.
    `%{&some_field}` - an indirect field where the key is indirectly sourced from the value of `some_field`.
    e.g. for a text of `error: some_error, description`, this `error: %{?err}, %{&desc}`will build a key/value of `'some_error' => description`
    Hint: use a Skip field if you do not want the indirection key/value stored.
    e.g. for a text of `google: 77.98`, this `%{?a}: %{&a}` will build a key/value of `google => 77.98`.

    Note: for append and indirect field the key can refer to a field that already exists in the event before dissection.
    Note: append and indirect cannot be combined. This will fail validation.
    `%{+&something}` - will add a value to the `&something` key, probably not the intended outcome.
    `%{&+something}` will add a value to the `+something` key, again unintended.
 */
public final class FieldFactory {
    private static final String MIXED_PREFIXES = "Field cannot prefix with both Append and Indirect Prefix (%s): %s";
    private static final String PREFIXED_EMPTY = "Field cannot be a prefix on its own without further text";
    private static final String NAME_EMPTY = "Field cannot be a prefix and a suffix without a name section";

    private FieldFactory() {
    }

    public static Field create(final int id, final String field, final Delimiter previous, final Delimiter next) {
        final String[] nameSuffix;
        if (field.isEmpty() || field.charAt(0) == '?') {
            nameSuffix = Field.extractNameSuffix(removeLeadingCharIfPresent(field));
            return SkipField.create(id, nameSuffix[0], nameSuffix[1], previous, next);
        }
        if (field.startsWith("+&")) {
            throw new InvalidFieldException(String.format(MIXED_PREFIXES, "+&", field));
        }
        if (field.startsWith("&+")) {
            throw new InvalidFieldException(String.format(MIXED_PREFIXES, "&+", field));
        }
        final String shorterField;
        if (field.charAt(0) == '+') {
            shorterField = removeLeadingCharIfPresent(field);
            if (shorterField.isEmpty()) {
                throw new InvalidFieldException(PREFIXED_EMPTY);
            }
            nameSuffix = Field.extractNameSuffix(shorterField);
            if (nameSuffix[0].isEmpty()) {
                throw new InvalidFieldException(NAME_EMPTY);
            }
            return AppendField.create(id, nameSuffix[0], nameSuffix[1], previous, next);
        }
        if (field.charAt(0) == '&') {
            shorterField = removeLeadingCharIfPresent(field);
            if (shorterField.isEmpty()) {
                throw new InvalidFieldException(PREFIXED_EMPTY);
            }
            nameSuffix = Field.extractNameSuffix(shorterField);
            if (nameSuffix[0].isEmpty()) {
                throw new InvalidFieldException(NAME_EMPTY);
            }
            return IndirectField.create(id, nameSuffix[0], nameSuffix[1], previous, next);
        }
        nameSuffix = Field.extractNameSuffix(field);
        return NormalField.create(id, nameSuffix[0], nameSuffix[1], previous, next);
    }

    private static String removeLeadingCharIfPresent(final String fieldName) {
        return fieldName.isEmpty() ? fieldName : fieldName.substring(1);
    }
}
