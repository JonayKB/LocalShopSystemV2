package es.jonay.kb.shopsystem.controller;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.TokenResponse;
import es.jonay.kb.shopsystem.api.exceptions.InvalidCredentialsException;
import es.jonay.kb.shopsystem.api.security.JwtService;
import es.jonay.kb.shopsystem.model.entities.RefreshToken;
import es.jonay.kb.shopsystem.model.entities.User;
import es.jonay.kb.shopsystem.model.repository.IRefreshTokenRepository;

/**
 * Login con token de acceso corto + refresh token rotatorio.
 * <ul>
 * <li>El refresh token es aleatorio (256 bits) y en BD solo se guarda su SHA-256.</li>
 * <li>Cada refresco revoca el token usado y emite uno nuevo.</li>
 * <li>Si llega un token ya revocado (posible robo), se cierran todas las sesiones del usuario.</li>
 * </ul>
 */
@Service
public class AuthController {

    static final String ROLE = "ROLE_ADMIN";
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Logger logger = Logger.getLogger(AuthController.class.getName());

    private final JwtService jwtService;
    private final UserController userService;
    private final PasswordEncoder passwordEncoder;
    private final IRefreshTokenRepository refreshTokens;
    private final Duration refreshExpiration;
    private final Clock clock;

    @Autowired
    public AuthController(JwtService jwtService, UserController userService, PasswordEncoder passwordEncoder,
            IRefreshTokenRepository refreshTokens,
            @Value("${jwt.refresh-expiration:2592000000}") long refreshExpirationMs) {
        this(jwtService, userService, passwordEncoder, refreshTokens, Duration.ofMillis(refreshExpirationMs),
                Clock.systemUTC());
    }

    AuthController(JwtService jwtService, UserController userService, PasswordEncoder passwordEncoder,
            IRefreshTokenRepository refreshTokens, Duration refreshExpiration, Clock clock) {
        this.jwtService = jwtService;
        this.userService = userService;
        this.passwordEncoder = passwordEncoder;
        this.refreshTokens = refreshTokens;
        this.refreshExpiration = refreshExpiration;
        this.clock = clock;
    }

    @Transactional
    public TokenResponse login(String mail, String password) {
        if (mail == null || password == null) {
            throw new InvalidCredentialsException();
        }
        User user = userService.findByEmail(mail.trim());
        if (user == null || !passwordEncoder.matches(password, user.getPassword())) {
            throw new InvalidCredentialsException();
        }
        return issue(user.getEmail());
    }

    // noRollbackFor: si se detecta reutilización, la revocación de todas las sesiones debe quedar guardada.
    @Transactional(noRollbackFor = InvalidCredentialsException.class)
    public TokenResponse refresh(String rawRefreshToken) {
        RefreshToken stored = find(rawRefreshToken);
        Instant now = clock.instant();
        if (stored.isRevoked()) {
            logger.warning("Refresh token reutilizado para " + stored.getUserEmail()
                    + ": se cierran todas sus sesiones");
            refreshTokens.revokeAllByUserEmail(stored.getUserEmail());
            throw new InvalidCredentialsException();
        }
        if (stored.isExpired(now)) {
            throw new InvalidCredentialsException();
        }
        if (userService.findByEmail(stored.getUserEmail()) == null) {
            stored.setRevoked(true);
            refreshTokens.save(stored);
            throw new InvalidCredentialsException();
        }
        stored.setRevoked(true);
        refreshTokens.save(stored);
        return issue(stored.getUserEmail());
    }

    /** Cierra la sesión de ese refresh token. Es idempotente: un token desconocido no da error. */
    @Transactional(propagation = Propagation.REQUIRED)
    public void logout(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            return;
        }
        refreshTokens.findByTokenHash(hash(rawRefreshToken)).ifPresent(t -> {
            t.setRevoked(true);
            refreshTokens.save(t);
        });
    }

    /** Borra de la BD los refresh tokens caducados. */
    @Transactional
    public int purgeExpired() {
        return refreshTokens.deleteExpired(clock.instant());
    }

    private RefreshToken find(String rawRefreshToken) {
        if (rawRefreshToken == null || rawRefreshToken.isBlank()) {
            throw new InvalidCredentialsException();
        }
        return refreshTokens.findByTokenHash(hash(rawRefreshToken)).orElseThrow(InvalidCredentialsException::new);
    }

    private TokenResponse issue(String email) {
        byte[] bytes = new byte[32];
        RANDOM.nextBytes(bytes);
        String raw = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        Instant now = clock.instant();
        refreshTokens.save(new RefreshToken(hash(raw), email, now, now.plus(refreshExpiration)));
        String access = jwtService.generateToken(email, ROLE);
        return new TokenResponse(access, raw, jwtService.getExpiration() / 1000);
    }

    static String hash(String raw) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(raw.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
