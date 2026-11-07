package es.jonay.kb.shopsystem.api.dto;

/** Credenciales en el cuerpo de la petición (nunca en la URL: acabarían en los logs). */
public record LoginRequest(String email, String password) {
}
