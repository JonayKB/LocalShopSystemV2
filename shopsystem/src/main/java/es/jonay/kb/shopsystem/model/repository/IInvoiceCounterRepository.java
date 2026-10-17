package es.jonay.kb.shopsystem.model.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.InvoiceCounter;
import jakarta.persistence.LockModeType;

@Repository
public interface IInvoiceCounterRepository extends JpaRepository<InvoiceCounter, String> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM InvoiceCounter c WHERE c.series = :series")
    Optional<InvoiceCounter> findBySeriesForUpdate(@Param("series") String series);
}
