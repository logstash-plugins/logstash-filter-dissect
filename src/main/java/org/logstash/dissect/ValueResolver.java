package org.logstash.dissect;

public final class ValueResolver {
    private final byte[] source;
    private final ValueRef[] fieldValues;

    ValueResolver(final byte[] source, final ValueRef[] fieldValues) {
        this.source = source;
        this.fieldValues = fieldValues;
    }

    public String get(final int fieldId) {
        return fieldValues[fieldId].extract(source);
    }

    public String getOtherByName(final String name, final int notFieldId) {
        int found = -1;
        for(int i = 0; i < fieldValues.length; i++) {
            final ValueRef ref = fieldValues[i];
            if (i != notFieldId && ref != null && name.contentEquals(ref.getFieldName())) {
                found = i;
                break;
            }
        }
        if (found >= 0) {
            return get(found);
        }
        return "";
    }
}
