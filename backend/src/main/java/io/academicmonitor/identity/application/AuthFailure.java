package io.academicmonitor.identity.application;

public class AuthFailure extends RuntimeException {
    public static final String INVALID_CREDENTIALS = "Correo o contraseña incorrectos.";

    public AuthFailure() {
        super(INVALID_CREDENTIALS);
    }
}
