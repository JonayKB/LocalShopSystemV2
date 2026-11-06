package es.jonay.kb.shopsystem.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Bucket;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.CategoryShare;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Inventory;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.ItemRank;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.PaymentShare;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Summary;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.TimePoint;
import es.jonay.kb.shopsystem.api.dto.analytics.SaleLineRow;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.PaymentMethod;
import es.jonay.kb.shopsystem.model.repository.IGraphsRepository;
import es.jonay.kb.shopsystem.model.repository.IItemPriceHistoryRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

/**
 * Cálculos de los informes con líneas sintéticas. El repositorio simulado filtra por fechas
 * igual que la consulta real, así se comprueban también los rangos y la zona horaria.
 */
class AnalyticsServiceTest {

    private static final ZoneId CANARY = ZoneId.of("Atlantic/Canary");
    private final List<SaleLineRow> all = new ArrayList<>();
    private IItemRepository items;
    private AnalyticsService analytics;

    @BeforeEach
    void setUp() {
        IGraphsRepository graphs = mock(IGraphsRepository.class);
        items = mock(IItemRepository.class);
        when(graphs.saleLines(any(), any())).thenAnswer(inv -> {
            Date from = inv.getArgument(0);
            Date to = inv.getArgument(1);
            return all.stream().filter(l -> !l.date().before(from) && l.date().before(to)).toList();
        });
        when(graphs.cancelledSummary(any(), any()))
                .thenReturn(java.util.Collections.singletonList(new Object[] { 1L, new BigDecimal("4.00") }));
        PricingService pricing = new PricingService(mock(IItemPriceHistoryRepository.class), items,
                new BigDecimal("7.00"));
        analytics = new AnalyticsService(graphs, items, pricing, "Atlantic/Canary");

        // Lunes 2 de junio de 2025 (hora de Canarias, UTC+1 en verano)
        line(1, "2025-06-02T09:15", PaymentMethod.CASH, 1L, "Agua", "Bebidas", 2, "2.40", "2.24", "0.16", "0.50");
        line(1, "2025-06-02T09:15", PaymentMethod.CASH, 2L, "Pan", "Panadería", 1, "1.00", "0.93", "0.07", null);
        line(2, "2025-06-02T18:40", PaymentMethod.CARD, 1L, "Agua", "Bebidas", 5, "6.00", "5.61", "0.39", "0.50");
        // Miércoles 4 de junio, 23:30 en Canarias = 22:30 UTC: debe contar como día 4 y hora 23.
        line(3, "2025-06-04T23:30", PaymentMethod.CASH, 3L, "Revista", null, 1, "3.00", "2.80", "0.20", "1.80");
        // Periodo anterior (fuera del rango 2-8 de junio)
        line(4, "2025-05-30T10:00", PaymentMethod.CASH, 1L, "Agua", "Bebidas", 1, "1.20", "1.12", "0.08", "0.50");
    }

    private void line(long trade, String localDateTime, PaymentMethod pm, Long itemId, String name, String category,
            int qty, String total, String base, String tax, String cost) {
        Date date = Date.from(LocalDateTime.parse(localDateTime).atZone(CANARY).toInstant());
        all.add(new SaleLineRow(trade, date, pm, itemId, name, category, qty, new BigDecimal(total),
                new BigDecimal(base), new BigDecimal(tax), cost == null ? null : new BigDecimal(cost)));
    }

    private static final LocalDate FROM = LocalDate.of(2025, 6, 2);
    private static final LocalDate TO = LocalDate.of(2025, 6, 8);

    @Test
    void resumenConBeneficioMargenYComparacion() {
        Summary s = analytics.summary(FROM, TO);

        assertThat(s.current().revenue()).isEqualByComparingTo("12.40");
        assertThat(s.current().taxBase()).isEqualByComparingTo("11.58");
        assertThat(s.current().tax()).isEqualByComparingTo("0.82");
        assertThat(s.current().trades()).isEqualTo(3);
        assertThat(s.current().units()).isEqualTo(9);
        assertThat(s.current().avgTicket()).isEqualByComparingTo("4.13");
        // Coste conocido: agua 7 x 0,50 + revista 1,80 = 5,30. Beneficio = (2,24+5,61+2,80) - 5,30
        assertThat(s.current().cost()).isEqualByComparingTo("5.30");
        assertThat(s.current().profit()).isEqualByComparingTo("5.35");
        // Margen sobre la base con coste (10,65) y cobertura de coste: 10,65 / 11,58
        assertThat(s.current().marginPct()).isEqualByComparingTo("50.2");
        assertThat(s.current().costCoverage()).isEqualByComparingTo("92.0");
        assertThat(s.current().cashRevenue()).isEqualByComparingTo("6.40");
        assertThat(s.current().cardRevenue()).isEqualByComparingTo("6.00");
        assertThat(s.current().cancelledTrades()).isEqualTo(1);

        // Periodo anterior: los 7 días previos (26 de mayo - 1 de junio)
        assertThat(s.previousFrom()).isEqualTo(LocalDate.of(2025, 5, 26));
        assertThat(s.previousTo()).isEqualTo(LocalDate.of(2025, 6, 1));
        assertThat(s.previous().revenue()).isEqualByComparingTo("1.20");
        assertThat(s.previous().trades()).isEqualTo(1);
    }

    @Test
    void evolucionDiariaRellenaLosDiasSinVentasYRespetaLaZonaHoraria() {
        List<TimePoint> days = analytics.salesOverTime(FROM, TO, "day");

        assertThat(days).hasSize(7);
        assertThat(days.get(0).period()).isEqualTo(FROM);
        assertThat(days.get(0).revenue()).isEqualByComparingTo("9.40");
        assertThat(days.get(0).trades()).isEqualTo(2);
        assertThat(days.get(1).revenue()).isEqualByComparingTo("0");
        assertThat(days.get(2).revenue()).isEqualByComparingTo("3.00"); // 23:30 del día 4 sigue siendo el día 4
    }

    @Test
    void evolucionSemanalYMensual() {
        List<TimePoint> weeks = analytics.salesOverTime(LocalDate.of(2025, 5, 26), TO, "week");
        assertThat(weeks).extracting(TimePoint::period).containsExactly(LocalDate.of(2025, 5, 26), FROM);
        assertThat(weeks.get(0).revenue()).isEqualByComparingTo("1.20");

        List<TimePoint> months = analytics.salesOverTime(LocalDate.of(2025, 5, 1), TO, "month");
        assertThat(months).extracting(TimePoint::period).containsExactly(LocalDate.of(2025, 5, 1),
                LocalDate.of(2025, 6, 1));
        assertThat(months.get(1).revenue()).isEqualByComparingTo("12.40");

        assertThatThrownBy(() -> analytics.salesOverTime(FROM, TO, "year")).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rankingDeProductosPorIngresosUnidadesOBeneficio() {
        List<ItemRank> byRevenue = analytics.topItems(FROM, TO, "revenue", 10);
        assertThat(byRevenue).extracting(ItemRank::name).containsExactly("Agua", "Revista", "Pan");
        ItemRank agua = byRevenue.get(0);
        assertThat(agua.units()).isEqualTo(7);
        assertThat(agua.revenue()).isEqualByComparingTo("8.40");
        assertThat(agua.profit()).isEqualByComparingTo("4.35"); // 7,85 - 3,50
        assertThat(agua.trades()).isEqualTo(2);
        // Sin coste conocido, el beneficio es null (no 0): no se confunde con "no gana nada".
        assertThat(byRevenue.get(2).profit()).isNull();

        assertThat(analytics.topItems(FROM, TO, "profit", 10)).extracting(ItemRank::name)
                .containsExactly("Agua", "Revista", "Pan");
        assertThat(analytics.topItems(FROM, TO, "units", 1)).extracting(ItemRank::name).containsExactly("Agua");
        assertThatThrownBy(() -> analytics.topItems(FROM, TO, "otra", 5)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void reparto_por_categoria_y_forma_de_pago() {
        List<CategoryShare> cats = analytics.byCategory(FROM, TO);
        assertThat(cats).extracting(CategoryShare::category).containsExactly("Bebidas", "Sin categoría", "Panadería");
        assertThat(cats.get(0).sharePct()).isEqualByComparingTo("67.7"); // 8,40 / 12,40

        List<PaymentShare> pay = analytics.byPaymentMethod(FROM, TO);
        assertThat(pay).extracting(PaymentShare::method).containsExactly("CASH", "CARD");
        assertThat(pay.get(0).trades()).isEqualTo(2);
        assertThat(pay.get(1).sharePct()).isEqualByComparingTo("48.4");
    }

    @Test
    void porHoraYDiaDeLaSemanaEnHoraLocal() {
        List<Bucket> hours = analytics.byHour(FROM, TO);
        assertThat(hours).hasSize(24);
        assertThat(hours.get(9).trades()).isEqualTo(1);
        assertThat(hours.get(18).revenue()).isEqualByComparingTo("6.00");
        assertThat(hours.get(23).revenue()).isEqualByComparingTo("3.00");

        List<Bucket> days = analytics.byWeekday(FROM, TO);
        assertThat(days.get(0).label()).isEqualTo("Lunes");
        assertThat(days.get(0).trades()).isEqualTo(2);
        assertThat(days.get(0).avgTicket()).isEqualByComparingTo("4.70");
        assertThat(days.get(2).revenue()).isEqualByComparingTo("3.00"); // miércoles
    }

    @Test
    void validaElRango() {
        assertThatThrownBy(() -> analytics.summary(TO, FROM)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> analytics.summary(LocalDate.of(2000, 1, 1), LocalDate.of(2025, 1, 1)))
                .isInstanceOf(IllegalArgumentException.class);
        // Sin fechas: últimos 30 días hasta hoy
        Summary s = analytics.summary(null, null);
        assertThat(s.to()).isEqualTo(LocalDate.now(CANARY));
        assertThat(s.from()).isEqualTo(LocalDate.now(CANARY).minusDays(29));
    }

    @Test
    void valorDelInventario() {
        Category bebidas = new Category(1L);
        bebidas.setName("Bebidas");
        bebidas.setIgicRate(new BigDecimal("7.00"));
        Item agua = item(1L, "Agua", bebidas, 10, 5, "1.07", "0.50", false);
        Item pan = item(2L, "Pan", null, 0, 2, "1.00", "0", false);
        Item bolsa = item(3L, "Bolsa", null, 999, 0, "0.10", "0.02", true); // ignora stock
        Item roto = item(4L, "Vaso", bebidas, 1, 3, "2.14", "1.00", false);
        when(items.findAllWithCategory()).thenReturn(List.of(agua, pan, bolsa, roto));

        Inventory inv = analytics.inventory();

        assertThat(inv.items()).isEqualTo(4);
        assertThat(inv.ignoringStock()).isEqualTo(1);
        assertThat(inv.units()).isEqualTo(11);
        assertThat(inv.valueAtCost()).isEqualByComparingTo("6.00"); // 10 x 0,50 + 1 x 1,00
        assertThat(inv.valueAtPrice()).isEqualByComparingTo("12.84"); // 10 x 1,07 + 1 x 2,14
        // Base: 1,00 y 2,00 -> (1,00-0,50) x 10 + (2,00-1,00) x 1
        assertThat(inv.potentialProfit()).isEqualByComparingTo("6.00");
        assertThat(inv.outOfStock()).isEqualTo(1);
        assertThat(inv.belowMinimum()).isEqualTo(1);
        assertThat(inv.itemsWithoutCost()).isEqualTo(1);
        assertThat(inv.alerts()).extracting(a -> a.name()).containsExactly("Pan", "Vaso");
    }

    private static Item item(Long id, String name, Category c, int stock, int min, String price, String cost,
            boolean ignore) {
        Item i = new Item(id, name, c, stock, min, ignore);
        i.setPrice(new BigDecimal(price));
        i.setCost(new BigDecimal(cost));
        return i;
    }
}
