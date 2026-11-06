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

    /**
     * Crea el contador si no existe y, en cualquier caso, deja su fila bloqueada en exclusiva
     * hasta el final de la transacción. Con un SELECT ... FOR UPDATE sobre una fila que aún no
     * existe, dos primeras ventas simultáneas se bloqueaban mutuamente (deadlock) al insertarla.
     */
    @org.springframework.data.jpa.repository.Modifying
    @Query(value = "INSERT INTO invoice_counters (series, last_number) VALUES (:series, 0) "
            + "ON DUPLICATE KEY UPDATE last_number = last_number", nativeQuery = true)
    int lockOrCreate(@Param("series") String series);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT c FROM InvoiceCounter c WHERE c.series = :series")
    Optional<InvoiceCounter> findBySeriesForUpdate(@Param("series") String series);
}
