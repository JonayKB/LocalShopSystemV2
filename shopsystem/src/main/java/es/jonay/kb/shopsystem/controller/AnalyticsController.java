package es.jonay.kb.shopsystem.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Bucket;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.CategoryShare;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Inventory;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.InventoryCategory;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.ItemRank;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.PaymentShare;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.StockAlert;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Summary;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.TimePoint;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Totals;
import es.jonay.kb.shopsystem.api.dto.analytics.SaleLineRow;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.PaymentMethod;
import es.jonay.kb.shopsystem.model.repository.IGraphsRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

/**
 * Informes del negocio. Todo se calcula con las líneas de venta congeladas (precio, IGIC,
 * coste y categoría del momento de la venta) y solo con ventas emitidas: las anuladas se
 * informan aparte. Los días se cuentan en la zona horaria de la tienda.
 *
 * Beneficio bruto = base imponible - coste * unidades, solo en líneas con coste conocido.
 */
@Service
public class AnalyticsController {

    static final int MAX_RANGE_DAYS = 3660;
    private static final BigDecimal HUNDRED = new BigDecimal("100");
    private static final String[] WEEKDAYS = { "Lunes", "Martes", "Miércoles", "Jueves", "Viernes", "Sábado",
            "Domingo" };

    private final IGraphsRepository graphs;
    private final IItemRepository items;
    private final PricingController pricing;
    private final ZoneId zone;

    public AnalyticsController(IGraphsRepository graphs, IItemRepository items, PricingController pricing,
            @Value("${shop.timezone:Atlantic/Canary}") String zone) {
        this.graphs = graphs;
        this.items = items;
        this.pricing = pricing;
        this.zone = ZoneId.of(zone);
    }

    /** Rango de fechas (ambas incluidas) en la zona de la tienda. */
    public record Range(LocalDate from, LocalDate to) {
        long days() {
            return ChronoUnit.DAYS.between(from, to) + 1;
        }
    }

    public Range range(LocalDate from, LocalDate to) {
        LocalDate today = LocalDate.now(zone);
        LocalDate t = to != null ? to : today;
        LocalDate f = from != null ? from : t.minusDays(29);
        if (f.isAfter(t)) {
            throw new IllegalArgumentException("La fecha de inicio es posterior a la de fin");
        }
        if (ChronoUnit.DAYS.between(f, t) + 1 > MAX_RANGE_DAYS) {
            throw new IllegalArgumentException("El rango máximo es de " + MAX_RANGE_DAYS + " días");
        }
        return new Range(f, t);
    }

    private List<SaleLineRow> lines(Range r) {
        return graphs.saleLines(start(r.from()), start(r.to().plusDays(1)));
    }

    private Date start(LocalDate day) {
        return Date.from(day.atStartOfDay(zone).toInstant());
    }

    private ZonedDateTime local(Date d) {
        return d.toInstant().atZone(zone);
    }

    // ------------------------------------------------------------------ resumen

    @Transactional(readOnly = true)
    public Summary summary(LocalDate from, LocalDate to) {
        Range r = range(from, to);
        Range prev = new Range(r.from().minusDays(r.days()), r.from().minusDays(1));
        return new Summary(r.from(), r.to(), totals(r), prev.from(), prev.to(), totals(prev));
    }

    private Totals totals(Range r) {
        Acc acc = new Acc();
        BigDecimal cash = BigDecimal.ZERO;
        BigDecimal card = BigDecimal.ZERO;
        for (SaleLineRow l : lines(r)) {
            acc.add(l);
            if (l.paymentMethod() == PaymentMethod.CARD) {
                card = card.add(l.lineTotal());
            } else {
                cash = cash.add(l.lineTotal());
            }
        }
        Object[] cancelled = graphs.cancelledSummary(start(r.from()), start(r.to().plusDays(1))).get(0);
        long trades = acc.trades.size();
        return new Totals(acc.revenue, acc.base, acc.tax, acc.cost, acc.profit, acc.marginPct(), acc.costCoverage(),
                trades, acc.units, div(acc.revenue, trades, 2),
                div(BigDecimal.valueOf(acc.units), trades, 1), cash, card, ((Number) cancelled[0]).longValue(),
                money((BigDecimal) cancelled[1]));
    }

    // ------------------------------------------------------------------ evolución

    @Transactional(readOnly = true)
    public List<TimePoint> salesOverTime(LocalDate from, LocalDate to, String granularity) {
        Range r = range(from, to);
        String g = granularity == null ? "day" : granularity.toLowerCase();
        if (!Set.of("day", "week", "month").contains(g)) {
            throw new IllegalArgumentException("granularity debe ser day, week o month");
        }
        // Todos los periodos del rango, aunque no tengan ventas (la gráfica no "salta" días).
        Map<LocalDate, Acc> buckets = new TreeMap<>();
        for (LocalDate d = bucketOf(r.from(), g); !d.isAfter(r.to()); d = next(d, g)) {
            buckets.put(d, new Acc());
        }
        for (SaleLineRow l : lines(r)) {
            buckets.computeIfAbsent(bucketOf(local(l.date()).toLocalDate(), g), k -> new Acc()).add(l);
        }
        List<TimePoint> out = new ArrayList<>();
        buckets.forEach((d, a) -> out.add(new TimePoint(d, a.revenue, a.profit, a.trades.size(), a.units)));
        return out;
    }

    private static LocalDate bucketOf(LocalDate d, String g) {
        return switch (g) {
            case "week" -> d.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            case "month" -> d.withDayOfMonth(1);
            default -> d;
        };
    }

    private static LocalDate next(LocalDate d, String g) {
        return switch (g) {
            case "week" -> d.plusWeeks(1);
            case "month" -> d.plusMonths(1);
            default -> d.plusDays(1);
        };
    }

    // ------------------------------------------------------------------ productos

    @Transactional(readOnly = true)
    public List<ItemRank> topItems(LocalDate from, LocalDate to, String metric, Integer limit) {
        Range r = range(from, to);
        String m = metric == null ? "revenue" : metric.toLowerCase();
        Comparator<ItemRank> order = switch (m) {
            case "units" -> Comparator.comparingLong(ItemRank::units).reversed();
            case "profit" -> Comparator.comparing((ItemRank i) -> i.profit() == null ? BigDecimal.valueOf(Long.MIN_VALUE)
                    : i.profit()).reversed();
            case "revenue" -> Comparator.comparing(ItemRank::revenue).reversed();
            default -> throw new IllegalArgumentException("metric debe ser revenue, units o profit");
        };
        int n = limit == null ? 10 : Math.max(1, Math.min(limit, 100));

        Map<String, Acc> byItem = new LinkedHashMap<>();
        Map<String, String[]> names = new LinkedHashMap<>();
        Map<String, Long> ids = new LinkedHashMap<>();
        for (SaleLineRow l : lines(r)) {
            String key = l.itemId() != null ? "id:" + l.itemId() : "name:" + l.description();
            byItem.computeIfAbsent(key, k -> new Acc()).add(l);
            names.put(key, new String[] { l.description(), category(l.categoryName()) }); // el nombre más reciente
            ids.put(key, l.itemId());
        }
        List<ItemRank> ranks = new ArrayList<>();
        byItem.forEach((k, a) -> ranks.add(new ItemRank(ids.get(k), names.get(k)[0], names.get(k)[1], a.units,
                a.revenue, a.withCost ? a.profit : null, a.marginPct(), a.trades.size())));
        ranks.sort(order.thenComparing(ItemRank::name, String.CASE_INSENSITIVE_ORDER));
        return ranks.subList(0, Math.min(n, ranks.size()));
    }

    // ------------------------------------------------------------------ categorías y pago

    @Transactional(readOnly = true)
    public List<CategoryShare> byCategory(LocalDate from, LocalDate to) {
        Map<String, Acc> by = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        BigDecimal total = BigDecimal.ZERO;
        for (SaleLineRow l : lines(range(from, to))) {
            by.computeIfAbsent(category(l.categoryName()), k -> new Acc()).add(l);
            total = total.add(l.lineTotal());
        }
        List<CategoryShare> out = new ArrayList<>();
        for (Map.Entry<String, Acc> e : by.entrySet()) {
            Acc a = e.getValue();
            out.add(new CategoryShare(e.getKey(), a.revenue, a.withCost ? a.profit : null, a.units, pct(a.revenue, total)));
        }
        out.sort(Comparator.comparing(CategoryShare::revenue).reversed());
        return out;
    }

    @Transactional(readOnly = true)
    public List<PaymentShare> byPaymentMethod(LocalDate from, LocalDate to) {
        Map<PaymentMethod, Acc> by = new LinkedHashMap<>();
        for (PaymentMethod pm : PaymentMethod.values()) {
            by.put(pm, new Acc());
        }
        BigDecimal total = BigDecimal.ZERO;
        for (SaleLineRow l : lines(range(from, to))) {
            PaymentMethod pm = l.paymentMethod() != null ? l.paymentMethod() : PaymentMethod.CASH;
            by.get(pm).add(l);
            total = total.add(l.lineTotal());
        }
        List<PaymentShare> out = new ArrayList<>();
        for (Map.Entry<PaymentMethod, Acc> e : by.entrySet()) {
            out.add(new PaymentShare(e.getKey().name(), e.getKey().label(), e.getValue().revenue,
                    e.getValue().trades.size(), pct(e.getValue().revenue, total)));
        }
        return out;
    }

    // ------------------------------------------------------------------ horas y días

    @Transactional(readOnly = true)
    public List<Bucket> byHour(LocalDate from, LocalDate to) {
        Acc[] h = new Acc[24];
        for (int i = 0; i < 24; i++) {
            h[i] = new Acc();
        }
        for (SaleLineRow l : lines(range(from, to))) {
            h[local(l.date()).getHour()].add(l);
        }
        List<Bucket> out = new ArrayList<>();
        for (int i = 0; i < 24; i++) {
            out.add(new Bucket(i, String.format("%02d:00", i), h[i].revenue, h[i].trades.size(),
                    div(h[i].revenue, h[i].trades.size(), 2)));
        }
        return out;
    }

    @Transactional(readOnly = true)
    public List<Bucket> byWeekday(LocalDate from, LocalDate to) {
        Acc[] w = new Acc[7];
        for (int i = 0; i < 7; i++) {
            w[i] = new Acc();
        }
        for (SaleLineRow l : lines(range(from, to))) {
            w[local(l.date()).getDayOfWeek().getValue() - 1].add(l);
        }
        List<Bucket> out = new ArrayList<>();
        for (int i = 0; i < 7; i++) {
            out.add(new Bucket(i + 1, WEEKDAYS[i], w[i].revenue, w[i].trades.size(),
                    div(w[i].revenue, w[i].trades.size(), 2)));
        }
        return out;
    }

    // ------------------------------------------------------------------ inventario

    /**
     * Valor del inventario actual. Los productos con "ignorar stock" no cuentan (su stock no
     * es real) y el stock negativo cuenta como 0.
     */
    @Transactional(readOnly = true)
    public Inventory inventory() {
        long count = 0;
        long units = 0;
        long withoutCost = 0;
        long outOfStock = 0;
        long below = 0;
        long ignoring = 0;
        BigDecimal atCost = BigDecimal.ZERO;
        BigDecimal atPrice = BigDecimal.ZERO;
        BigDecimal potential = BigDecimal.ZERO;
        Map<String, Object[]> byCat = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        List<StockAlert> alerts = new ArrayList<>();

        for (Item i : items.findAllWithCategory()) {
            count++;
            if (Boolean.TRUE.equals(i.getIgnoreStock())) {
                ignoring++;
                continue;
            }
            int stock = i.getStock() == null ? 0 : i.getStock();
            int min = i.getBareMinimun() == null ? 0 : i.getBareMinimun();
            String cat = i.getCategory() != null ? i.getCategory().getName() : "Sin categoría";
            if (stock <= 0) {
                outOfStock++;
                alerts.add(new StockAlert(i.getId(), i.getName(), cat, stock, min));
            } else if (min > 0 && stock < min) {
                below++;
                alerts.add(new StockAlert(i.getId(), i.getName(), cat, stock, min));
            }
            BigDecimal cost = i.getCost() == null ? BigDecimal.ZERO : i.getCost();
            BigDecimal price = i.getPrice() == null ? BigDecimal.ZERO : i.getPrice();
            if (cost.signum() == 0) {
                withoutCost++;
            }
            long u = Math.max(0, stock);
            BigDecimal qty = BigDecimal.valueOf(u);
            BigDecimal c = cost.multiply(qty);
            BigDecimal p = price.multiply(qty);
            units += u;
            atCost = atCost.add(c);
            atPrice = atPrice.add(p);
            if (cost.signum() > 0 && u > 0) {
                BigDecimal rate = pricing.igicRateOf(i.getCategory());
                BigDecimal base = price.divide(BigDecimal.ONE.add(rate.divide(HUNDRED, 6, RoundingMode.HALF_UP)), 6,
                        RoundingMode.HALF_UP);
                potential = potential.add(base.subtract(cost).multiply(qty));
            }
            Object[] acc = byCat.computeIfAbsent(cat, k -> new Object[] { 0L, 0L, BigDecimal.ZERO, BigDecimal.ZERO });
            acc[0] = (Long) acc[0] + 1;
            acc[1] = (Long) acc[1] + u;
            acc[2] = ((BigDecimal) acc[2]).add(c);
            acc[3] = ((BigDecimal) acc[3]).add(p);
        }
        List<InventoryCategory> cats = new ArrayList<>();
        byCat.forEach((k, v) -> cats.add(new InventoryCategory(k, (Long) v[0], (Long) v[1], money((BigDecimal) v[2]),
                money((BigDecimal) v[3]))));
        cats.sort(Comparator.comparing(InventoryCategory::valueAtCost).reversed());
        // Primero los agotados (más negativos arriba), luego los que están bajo mínimo.
        alerts.sort(Comparator.comparingInt((StockAlert a) -> a.stock() <= 0 ? 0 : 1)
                .thenComparingInt(a -> a.stock() - a.bareMinimun())
                .thenComparing(StockAlert::name, String.CASE_INSENSITIVE_ORDER));
        return new Inventory(count, units, money(atCost), money(atPrice), money(potential), withoutCost, outOfStock,
                below, ignoring, cats, alerts.subList(0, Math.min(100, alerts.size())));
    }

    // ------------------------------------------------------------------ utilidades

    private static String category(String name) {
        return name != null && !name.isBlank() ? name : "Sin categoría";
    }

    static BigDecimal money(BigDecimal v) {
        return (v == null ? BigDecimal.ZERO : v).setScale(2, RoundingMode.HALF_UP);
    }

    private static BigDecimal div(BigDecimal a, long b, int scale) {
        return b == 0 ? BigDecimal.ZERO.setScale(scale) : a.divide(BigDecimal.valueOf(b), scale, RoundingMode.HALF_UP);
    }

    private static BigDecimal pct(BigDecimal part, BigDecimal total) {
        return total.signum() == 0 ? BigDecimal.ZERO.setScale(1)
                : part.multiply(HUNDRED).divide(total, 1, RoundingMode.HALF_UP);
    }

    /** Acumulador de líneas. */
    private static final class Acc {
        BigDecimal revenue = BigDecimal.ZERO.setScale(2);
        BigDecimal base = BigDecimal.ZERO.setScale(2);
        BigDecimal tax = BigDecimal.ZERO.setScale(2);
        BigDecimal cost = BigDecimal.ZERO.setScale(2);
        BigDecimal profit = BigDecimal.ZERO.setScale(2);
        /** Base imponible de las líneas con coste conocido. */
        BigDecimal baseWithCost = BigDecimal.ZERO.setScale(2);
        long units;
        boolean withCost;
        final Set<Long> trades = new HashSet<>();

        void add(SaleLineRow l) {
            revenue = revenue.add(l.lineTotal());
            base = base.add(l.taxBase());
            tax = tax.add(l.taxAmount());
            units += l.quantity();
            trades.add(l.tradeId());
            if (l.unitCost() != null && l.unitCost().signum() > 0) {
                BigDecimal c = l.unitCost().multiply(BigDecimal.valueOf(l.quantity())).setScale(2, RoundingMode.HALF_UP);
                cost = cost.add(c);
                profit = profit.add(l.taxBase().subtract(c));
                baseWithCost = baseWithCost.add(l.taxBase());
                withCost = true;
            }
        }

        BigDecimal marginPct() {
            return baseWithCost.signum() == 0 ? null : profit.multiply(HUNDRED).divide(baseWithCost, 1, RoundingMode.HALF_UP);
        }

        BigDecimal costCoverage() {
            return pct(baseWithCost, base);
        }
    }
}
