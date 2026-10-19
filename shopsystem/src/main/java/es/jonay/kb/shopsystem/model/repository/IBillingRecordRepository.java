package es.jonay.kb.shopsystem.model.repository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.BillingRecord;
import es.jonay.kb.shopsystem.model.entities.BillingRecordType;
import es.jonay.kb.shopsystem.model.entities.BillingSendStatus;

@Repository
public interface IBillingRecordRepository extends JpaRepository<BillingRecord, Long> {

    Optional<BillingRecord> findFirstByTradeIdAndTypeOrderByIdDesc(Long tradeId, BillingRecordType type);

    boolean existsByTradeIdAndType(Long tradeId, BillingRecordType type);

    List<BillingRecord> findByTradeIdIn(Collection<Long> tradeIds);

    /** Cola de envío en orden de cadena. Usar con PageRequest.of(0, 1000). */
    List<BillingRecord> findByStatusOrderByIdAsc(BillingSendStatus status, Pageable pageable);

    Optional<BillingRecord> findFirstByStatusOrderByIdAsc(BillingSendStatus status);

    long countByStatus(BillingSendStatus status);

    @Query("SELECT r.status, COUNT(r) FROM BillingRecord r GROUP BY r.status")
    List<Object[]> countGroupedByStatus();
}
