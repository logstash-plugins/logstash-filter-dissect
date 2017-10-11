package org.logstash.dissect.fields;

public class InvalidFieldException extends RuntimeException {
    public InvalidFieldException() {
        super();
    }

    public InvalidFieldException(final String message) {
        super(message);
    }

    public InvalidFieldException(final String message, final Throwable cause) {
        super(message, cause);
    }

    public InvalidFieldException(final Throwable cause) {
        super(cause);
    }
}
