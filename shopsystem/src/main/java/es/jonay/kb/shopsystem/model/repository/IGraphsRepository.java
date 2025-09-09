package es.jonay.kb.shopsystem.model.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import es.jonay.kb.shopsystem.model.entities.Trade;

@Repository
public interface IGraphsRepository extends JpaRepository<Trade, Long> {

    // ================== VENTAS ==================

    // Productos más vendidos
    @Query("""
            SELECT i.name, COUNT(tr)
            FROM Trade tr
            JOIN tr.items i
            GROUP BY i.name
            ORDER BY COUNT(tr) DESC
            """)
    List<Object[]> topSoldItems();

    // Evolución de trades en el tiempo
    @Query("""
            SELECT DATE(tr.date), COUNT(tr)
            FROM Trade tr
            GROUP BY DATE(tr.date)
            ORDER BY DATE(tr.date)
            """)
    List<Object[]> countTradesByDay();

    // Valor promedio de los trades
    @Query("""
            SELECT AVG(ph)
            FROM Trade tr
            JOIN tr.items i
            JOIN i.priceHistory ph
            WHERE KEY(ph) <= tr.date
            """)
    Double averageTradeValue();

    @Query("""
            SELECT c.name, SUM(ph)
            FROM Trade tr
            JOIN tr.items i
            JOIN i.category c
            JOIN i.priceHistory ph
            WHERE KEY(ph) <= tr.date
            GROUP BY c.name
            """)
    List<Object[]> incomeByCategory();

    // ================== INVENTARIO ==================


    // Distribución de stock por categoría
    @Query("""
            SELECT c.name, SUM(i.stock)
            FROM Item i
            JOIN i.category c
            GROUP BY c.name
            """)
    List<Object[]> stockByCategory();

    // Productos que ignoran stock
    @Query("""
            SELECT i.name
            FROM Item i
            WHERE i.ignoreStock = true
            """)
    List<Object[]> itemsIgnoringStock();

    // ================== FINANZAS ==================

    // Productos con mayor margen de beneficio (neto acumulado)
    @Query("""
            SELECT i.name, SUM(nh)
            FROM Trade tr
            JOIN tr.items i
            JOIN i.netHistory nh
            WHERE KEY(nh) <= tr.date
            AND nh <> 0
            GROUP BY i.name
            ORDER BY SUM(nh) DESC
            """)
    List<Object[]> topProfitItems();

    // Evolución del beneficio acumulado en el tiempo
    @Query("""
            SELECT DATE(KEY(nh)), SUM(nh)
            FROM Item i
            JOIN i.netHistory nh
            WHERE nh <> 0
            GROUP BY DATE(KEY(nh))
            ORDER BY DATE(KEY(nh))
            """)
    List<Object[]> accumulatedProfitOverTime();

}
