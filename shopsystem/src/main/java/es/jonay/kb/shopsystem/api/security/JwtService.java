package es.jonay.kb.shopsystem.api.security;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.Claim;

/**
 * Tokens de acceso (JWT, vida corta). Los refresh tokens no son JWT: son valores
 * aleatorios guardados en base de datos (ver {@code AuthController}).
 */
@Service
public class JwtService {

    static final String TYPE_CLAIM = "type";
    static final String ACCESS_TYPE = "access";

    private final Algorithm algorithm;
    private final long expiration;

    public JwtService(@Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration:900000}") long expiration) {
        if (secret == null || secret.length() < 32) {
            throw new IllegalStateException("jwt.secret debe tener al menos 32 caracteres");
        }
        this.algorithm = Algorithm.HMAC256(secret);
        this.expiration = expiration;
    }

    /** Vida del token de acceso en milisegundos. */
    public long getExpiration() {
        return expiration;
    }

    public String generateToken(String mail, String rol) {
        return JWT.create()
                .withSubject(mail)
                .withClaim("role", rol)
                .withClaim(TYPE_CLAIM, ACCESS_TYPE)
                .withIssuedAt(new Date())
                .withExpiresAt(new Date(System.currentTimeMillis() + expiration))
                .sign(algorithm);
    }

    /** Valida firma, caducidad y que sea un token de acceso. Lanza JWTVerificationException si no. */
    public Map<String, String> validateAndGetClaims(String token) {
        Map<String, Claim> claims = JWT.require(algorithm)
                .withClaim(TYPE_CLAIM, ACCESS_TYPE)
                .build()
                .verify(token)
                .getClaims();

        Map<String, String> infoToken = new HashMap<>();
        infoToken.put("mail", claims.get("sub").asString());
        infoToken.put("role", claims.get("role").asString());

        return infoToken;
    }
}
