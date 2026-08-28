package io.academicmonitor.integration.idukay.auth;

public class IdukayLoginException extends RuntimeException {

    public IdukayLoginException(String message) {
        super(message);
    }

    public IdukayLoginException(String message, Throwable cause) {

        super(message, cause);
    }
}
