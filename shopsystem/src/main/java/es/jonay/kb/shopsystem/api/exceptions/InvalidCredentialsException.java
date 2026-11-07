package es.jonay.kb.shopsystem.api.exceptions;

/** Credenciales o refresh token no válidos. El mensaje es genérico a propósito. */
public class InvalidCredentialsException extends RuntimeException {
    public InvalidCredentialsException() {
        super("Usuario o contraseña incorrectos, o sesión caducada");
    }
}
