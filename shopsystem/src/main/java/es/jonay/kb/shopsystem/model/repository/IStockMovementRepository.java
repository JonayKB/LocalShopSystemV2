package es.jonay.kb.shopsystem.model.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.StockMovement;
import es.jonay.kb.shopsystem.model.entities.StockMovementType;

@Repository
public interface IStockMovementRepository extends JpaRepository<StockMovement, Long> {

    Page<StockMovement> findByItemIdOrderByCreatedAtDescIdDesc(Long itemId, Pageable pageable);

    List<StockMovement> findByTradeId(Long tradeId);

    /** Libro de movimientos con filtros opcionales (null = sin filtro). */
    @Query("""
            SELECT m FROM StockMovement m
            WHERE (:itemId IS NULL OR m.itemId = :itemId)
              AND (:type IS NULL OR m.type = :type)
              AND (:from IS NULL OR m.createdAt >= :from)
              AND (:to IS NULL OR m.createdAt < :to)
            ORDER BY m.createdAt DESC, m.id DESC
            """)
    Page<StockMovement> search(@Param("itemId") Long itemId, @Param("type") StockMovementType type,
            @Param("from") Instant from, @Param("to") Instant to, Pageable pageable);

    @Query("SELECT COALESCE(SUM(m.quantity), 0) FROM StockMovement m WHERE m.itemId = :itemId")
    long sumQuantityByItemId(@Param("itemId") Long itemId);
}
