package es.jonay.kb.shopsystem.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import es.jonay.kb.shopsystem.api.dto.TokenResponse;
import es.jonay.kb.shopsystem.api.exceptions.InvalidCredentialsException;
import es.jonay.kb.shopsystem.api.security.JwtService;
import es.jonay.kb.shopsystem.model.entities.RefreshToken;
import es.jonay.kb.shopsystem.model.entities.User;
import es.jonay.kb.shopsystem.model.repository.IRefreshTokenRepository;

class AuthControllerTest {

    private static final Instant NOW = Instant.parse("2026-01-01T10:00:00Z");

    private final PasswordEncoder encoder = new BCryptPasswordEncoder(4);
    private final JwtService jwt = new JwtService("secreto-de-pruebas-con-mas-de-32-caracteres", 900_000);
    private UserController users;
    private IRefreshTokenRepository repo;
    /** "Base de datos" de refresh tokens en memoria, por hash. */
    private final Map<String, RefreshToken> stored = new HashMap<>();
    private AuthController auth;

    @BeforeEach
    void setUp() {
        users = mock(UserController.class);
        repo = mock(IRefreshTokenRepository.class);
        when(users.findByEmail("admin@test")).thenReturn(new User(1L, "admin@test", encoder.encode("clave")));
        when(repo.save(any(RefreshToken.class))).thenAnswer(inv -> {
            RefreshToken t = inv.getArgument(0);
            stored.put(t.getTokenHash(), t);
            return t;
        });
        when(repo.findByTokenHash(any())).thenAnswer(inv -> Optional.ofNullable(stored.get(inv.getArgument(0))));
        when(repo.revokeAllByUserEmail("admin@test")).thenAnswer(inv -> {
            stored.values().forEach(t -> t.setRevoked(true));
            return stored.size();
        });
        auth = newAuth(Clock.fixed(NOW, ZoneOffset.UTC));
    }

    private AuthController newAuth(Clock clock) {
        return new AuthController(jwt, users, encoder, repo, Duration.ofDays(30), clock);
    }

    @Test
    void loginCorrectoDevuelveTokenDeAccesoYRefresh() {
        TokenResponse r = auth.login("admin@test", "clave");

        assertThat(jwt.validateAndGetClaims(r.accessToken())).containsEntry("mail", "admin@test");
        assertThat(r.refreshToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(r.expiresIn()).isEqualTo(900);
    }

    @Test
    void elRefreshTokenSeGuardaSoloComoHash() {
        TokenResponse r = auth.login("admin@test", "clave");

        RefreshToken t = stored.values().iterator().next();
        assertThat(t.getTokenHash()).isEqualTo(AuthController.hash(r.refreshToken())).isNotEqualTo(r.refreshToken());
        assertThat(t.getExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
    }

    @Test
    void loginIncorrectoFallaConMensajeGenerico() {
        assertThatThrownBy(() -> auth.login("admin@test", "mala")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.login("nadie@test", "clave")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.login(null, null)).isInstanceOf(InvalidCredentialsException.class);
        verify(repo, never()).save(any());
    }

    @Test
    void refreshRotaElToken() {
        TokenResponse first = auth.login("admin@test", "clave");

        TokenResponse second = auth.refresh(first.refreshToken());

        assertThat(second.refreshToken()).isNotEqualTo(first.refreshToken());
        assertThat(stored.get(AuthController.hash(first.refreshToken())).isRevoked()).isTrue();
        assertThat(stored.get(AuthController.hash(second.refreshToken())).isRevoked()).isFalse();
        assertThat(jwt.validateAndGetClaims(second.accessToken())).containsEntry("mail", "admin@test");
    }

    @Test
    void reutilizarUnRefreshYaUsadoCierraTodasLasSesiones() {
        TokenResponse first = auth.login("admin@test", "clave");
        TokenResponse second = auth.refresh(first.refreshToken());

        // Alguien reutiliza el token viejo (robado): se rechaza y se revoca también el nuevo.
        assertThatThrownBy(() -> auth.refresh(first.refreshToken())).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.refresh(second.refreshToken())).isInstanceOf(InvalidCredentialsException.class);
        verify(repo, atLeastOnce()).revokeAllByUserEmail("admin@test");
    }

    @Test
    void refreshCaducadoORaroSeRechaza() {
        TokenResponse r = auth.login("admin@test", "clave");
        AuthController later = newAuth(Clock.fixed(NOW.plus(Duration.ofDays(31)), ZoneOffset.UTC));

        assertThatThrownBy(() -> later.refresh(r.refreshToken())).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.refresh("inventado")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.refresh("")).isInstanceOf(InvalidCredentialsException.class);
        assertThatThrownBy(() -> auth.refresh(null)).isInstanceOf(InvalidCredentialsException.class);
    }

    @Test
    void refreshDeUnUsuarioBorradoSeRechaza() {
        TokenResponse r = auth.login("admin@test", "clave");
        when(users.findByEmail("admin@test")).thenReturn(null);

        assertThatThrownBy(() -> auth.refresh(r.refreshToken())).isInstanceOf(InvalidCredentialsException.class);
        assertThat(stored.get(AuthController.hash(r.refreshToken())).isRevoked()).isTrue();
    }

    @Test
    void logoutRevocaElRefreshYEsIdempotente() {
        TokenResponse r = auth.login("admin@test", "clave");

        auth.logout(r.refreshToken());
        auth.logout(r.refreshToken());
        auth.logout("desconocido");
        auth.logout(null);

        assertThatThrownBy(() -> auth.refresh(r.refreshToken())).isInstanceOf(InvalidCredentialsException.class);
    }
}
