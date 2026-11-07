package es.jonay.kb.shopsystem.api.dto.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Respuestas de los informes. Importes en euros con 2 decimales.
 * Beneficio = base imponible - coste * unidades, solo de las líneas con coste conocido
 * (costCoverage = % de la base imponible que tiene coste conocido).
 */
public final class AnalyticsDtos {

    private AnalyticsDtos() {
    }

    public record Totals(BigDecimal revenue, BigDecimal taxBase, BigDecimal tax, BigDecimal cost,
            BigDecimal profit, BigDecimal marginPct, BigDecimal costCoverage, long trades, long units,
            BigDecimal avgTicket, BigDecimal avgUnitsPerTrade, BigDecimal cashRevenue, BigDecimal cardRevenue,
            long cancelledTrades, BigDecimal cancelledAmount) {
    }

    /** KPIs del periodo y del periodo anterior de la misma duración (para comparar). */
    public record Summary(LocalDate from, LocalDate to, Totals current, LocalDate previousFrom,
            LocalDate previousTo, Totals previous) {
    }

    /** Un punto de la serie temporal: periodo = inicio del día, semana (lunes) o mes. */
    public record TimePoint(LocalDate period, BigDecimal revenue, BigDecimal profit, long trades, long units) {
    }

    public record ItemRank(Long itemId, String name, String category, long units, BigDecimal revenue,
            BigDecimal profit, BigDecimal marginPct, long trades) {
    }

    public record CategoryShare(String category, BigDecimal revenue, BigDecimal profit, long units,
            BigDecimal sharePct) {
    }

    public record PaymentShare(String method, String label, BigDecimal revenue, long trades, BigDecimal sharePct) {
    }

    /** bucket: hora 0-23 o día de la semana 1 (lunes) - 7 (domingo). */
    public record Bucket(int bucket, String label, BigDecimal revenue, long trades, BigDecimal avgTicket) {
    }

    public record InventoryCategory(String category, long items, long units, BigDecimal valueAtCost,
            BigDecimal valueAtPrice) {
    }

    public record StockAlert(Long itemId, String name, String category, int stock, int bareMinimun) {
    }

    public record Inventory(long items, long units, BigDecimal valueAtCost, BigDecimal valueAtPrice,
            BigDecimal potentialProfit, long itemsWithoutCost, long outOfStock, long belowMinimum,
            long ignoringStock, List<InventoryCategory> byCategory, List<StockAlert> alerts) {
    }
}
