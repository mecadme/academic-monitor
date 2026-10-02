package io.academicmonitor.integration.idukay.client;

public class IdukayApiException extends RuntimeException {

    private final Integer statusCode;

    public IdukayApiException(String message) {

        super(message);
        statusCode = null;
    }

    public IdukayApiException(String message, Throwable cause) {

        super(message, cause);
        statusCode = null;
    }

    public IdukayApiException(String message, int statusCode, Throwable cause) {

        super(message, cause);
        this.statusCode = statusCode;
    }

    public Integer getStatusCode() {
        return statusCode;
    }
}
