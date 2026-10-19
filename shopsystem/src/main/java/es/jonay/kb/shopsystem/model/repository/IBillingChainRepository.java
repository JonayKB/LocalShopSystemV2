package es.jonay.kb.shopsystem.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.BillingChain;
import jakarta.persistence.LockModeType;

@Repository
public interface IBillingChainRepository extends JpaRepository<BillingChain, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM BillingChain c WHERE c.id = :id")
    Optional<BillingChain> findByIdForUpdate(@Param("id") String id);
}
