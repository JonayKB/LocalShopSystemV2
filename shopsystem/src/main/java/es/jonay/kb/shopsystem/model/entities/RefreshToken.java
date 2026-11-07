package es.jonay.kb.shopsystem.model.entities;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;

/**
 * Refresh token de una sesión. Solo se guarda el hash SHA-256 del token: si alguien
 * lee la base de datos no puede usarlos. Cada uso lo rota (se revoca y se emite otro).
 */
@Entity
@Table(name = "refresh_tokens", indexes = @Index(name = "idx_refresh_token_email", columnList = "userEmail"))
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(nullable = false)
    private String userEmail;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(nullable = false)
    private Instant expiresAt;

    @Column(nullable = false)
    private boolean revoked = false;

    public RefreshToken() {
    }

    public RefreshToken(String tokenHash, String userEmail, Instant createdAt, Instant expiresAt) {
        this.tokenHash = tokenHash;
        this.userEmail = userEmail;
        this.createdAt = createdAt;
        this.expiresAt = expiresAt;
    }

    public boolean isExpired(Instant now) {
        return !expiresAt.isAfter(now);
    }

    public Long getId() {
        return id;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public String getUserEmail() {
        return userEmail;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public boolean isRevoked() {
        return revoked;
    }

    public void setRevoked(boolean revoked) {
        this.revoked = revoked;
    }
}
