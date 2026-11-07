package es.jonay.kb.shopsystem.web;

import java.time.LocalDate;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Bucket;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.CategoryShare;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Inventory;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.ItemRank;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.PaymentShare;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.Summary;
import es.jonay.kb.shopsystem.api.dto.analytics.AnalyticsDtos.TimePoint;
import es.jonay.kb.shopsystem.service.AnalyticsService;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Informes. from/to = fechas (yyyy-MM-dd, ambas incluidas, zona de la tienda).
 * Por defecto, los últimos 30 días. Sin caché: los datos son siempre los del momento.
 */
@RestController
@RequestMapping("graphs")
@Tag(name = "GRAPHS", description = "Informes de ventas, beneficio e inventario")
public class AnalyticsController {

    private final AnalyticsService analytics;

    public AnalyticsController(AnalyticsService analytics) {
        this.analytics = analytics;
    }

    @GetMapping("summary")
    public Summary summary(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.summary(from, to);
    }

    @GetMapping("sales-over-time")
    public List<TimePoint> salesOverTime(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "day") String granularity) {
        return analytics.salesOverTime(from, to, granularity);
    }

    @GetMapping("top-items")
    public List<ItemRank> topItems(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            @RequestParam(defaultValue = "revenue") String metric,
            @RequestParam(defaultValue = "10") Integer limit) {
        return analytics.topItems(from, to, metric, limit);
    }

    @GetMapping("by-category")
    public List<CategoryShare> byCategory(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.byCategory(from, to);
    }

    @GetMapping("by-payment-method")
    public List<PaymentShare> byPaymentMethod(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.byPaymentMethod(from, to);
    }

    @GetMapping("by-hour")
    public List<Bucket> byHour(@RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.byHour(from, to);
    }

    @GetMapping("by-weekday")
    public List<Bucket> byWeekday(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return analytics.byWeekday(from, to);
    }

    @GetMapping("inventory")
    public Inventory inventory() {
        return analytics.inventory();
    }
}
