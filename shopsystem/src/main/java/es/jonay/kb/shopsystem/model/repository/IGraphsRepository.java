package es.jonay.kb.shopsystem.model.repository;

import java.util.Date;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.api.dto.analytics.SaleLineRow;
import es.jonay.kb.shopsystem.model.entities.Trade;

/**
 * Datos para informes y gráficas. Todo sale de las líneas de venta congeladas (precio,
 * IGIC, coste y categoría del momento de la venta), nunca de los precios actuales.
 */
@Repository
public interface IGraphsRepository extends JpaRepository<Trade, Long> {

    /** Líneas de ventas emitidas (no anuladas) en [from, to). */
    @Query("""
            SELECT new es.jonay.kb.shopsystem.api.dto.analytics.SaleLineRow(
                t.id, t.date, t.paymentMethod, l.itemId, l.description, l.categoryName,
                l.quantity, l.lineTotal, l.taxBase, l.taxAmount, l.unitCost)
            FROM TradeLine l JOIN l.trade t
            WHERE t.status = es.jonay.kb.shopsystem.model.entities.TradeStatus.ISSUED
              AND t.date >= :from AND t.date < :to
            """)
    List<SaleLineRow> saleLines(@Param("from") Date from, @Param("to") Date to);

    /** Ventas anuladas en [from, to) (por fecha de la venta): número e importe. */
    @Query("""
            SELECT COUNT(t), COALESCE(SUM(t.total), 0)
            FROM Trade t
            WHERE t.status = es.jonay.kb.shopsystem.model.entities.TradeStatus.CANCELLED
              AND t.date >= :from AND t.date < :to
            """)
    List<Object[]> cancelledSummary(@Param("from") Date from, @Param("to") Date to);
}
