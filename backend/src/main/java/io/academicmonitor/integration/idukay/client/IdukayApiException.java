package io.academicmonitor.integration.idukay.client;

public class IdukayApiException extends RuntimeException {

    public IdukayApiException(String message) {

        super(message);
    }

    public IdukayApiException(String message, Throwable cause) {

        super(message, cause);
    }
}
