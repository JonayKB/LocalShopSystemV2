package es.jonay.kb.shopsystem.api.dto;

/** Respuesta de login y refresco. expiresIn = segundos de vida del token de acceso. */
public record TokenResponse(String accessToken, String refreshToken, long expiresIn) {
}
