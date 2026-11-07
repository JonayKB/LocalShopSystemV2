package es.jonay.kb.shopsystem.api.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Date;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;

class JwtServiceTest {

    private static final String SECRET = "secreto-de-pruebas-con-mas-de-32-caracteres";

    @Test
    void generaYValidaUnTokenDeAcceso() {
        JwtService service = new JwtService(SECRET, 60_000);

        String token = service.generateToken("admin@test", "ROLE_ADMIN");
        Map<String, String> claims = service.validateAndGetClaims(token);

        assertThat(claims).containsEntry("mail", "admin@test").containsEntry("role", "ROLE_ADMIN");
        assertThat(service.getExpiration()).isEqualTo(60_000);
    }

    @Test
    void elTokenCaducaSegunLaExpiracionConfigurada() {
        JwtService service = new JwtService(SECRET, 15 * 60_000);
        String token = service.generateToken("admin@test", "ROLE_ADMIN");

        long seconds = (JWT.decode(token).getExpiresAt().getTime() - System.currentTimeMillis()) / 1000;
        assertThat(seconds).isBetween(14 * 60L, 15 * 60L);
    }

    @Test
    void rechazaTokenCaducado() {
        JwtService service = new JwtService(SECRET, -1_000);
        String token = service.generateToken("admin@test", "ROLE_ADMIN");

        assertThatThrownBy(() -> service.validateAndGetClaims(token)).isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void rechazaTokenFirmadoConOtroSecreto() {
        String token = new JwtService("otro-secreto-distinto-con-mas-de-32-caracteres", 60_000)
                .generateToken("admin@test", "ROLE_ADMIN");

        assertThatThrownBy(() -> new JwtService(SECRET, 60_000).validateAndGetClaims(token))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void rechazaTokensAntiguosSinTipoDeAcceso() {
        // Los tokens emitidos antes de este cambio no llevan type=access: deben dejar de valer.
        String legacy = JWT.create().withSubject("admin@test").withClaim("role", "ROLE_ADMIN")
                .withExpiresAt(new Date(System.currentTimeMillis() + 60_000)).sign(Algorithm.HMAC256(SECRET));

        assertThatThrownBy(() -> new JwtService(SECRET, 60_000).validateAndGetClaims(legacy))
                .isInstanceOf(JWTVerificationException.class);
    }

    @Test
    void exigeUnSecretoLargo() {
        assertThatThrownBy(() -> new JwtService("corto", 60_000)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> new JwtService(null, 60_000)).isInstanceOf(IllegalStateException.class);
    }
}
