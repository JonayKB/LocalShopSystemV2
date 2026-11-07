package es.jonay.kb.shopsystem.model.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.RefreshToken;

@Repository
public interface IRefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Cierra todas las sesiones de un usuario (p. ej. si se reutiliza un token ya rotado). */
    @Modifying
    @Query("UPDATE RefreshToken r SET r.revoked = true WHERE r.userEmail = :email AND r.revoked = false")
    int revokeAllByUserEmail(@Param("email") String email);

    @Modifying
    @Query("DELETE FROM RefreshToken r WHERE r.expiresAt < :now")
    int deleteExpired(@Param("now") Instant now);
}
