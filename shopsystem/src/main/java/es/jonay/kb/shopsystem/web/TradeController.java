package es.jonay.kb.shopsystem.web;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.DailyReportDto;
import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.SaleRequest;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.service.TicketPrinterService;
import es.jonay.kb.shopsystem.service.TradeService;
import es.jonay.kb.shopsystem.model.entities.PaymentMethod;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("trade")
public class TradeController {
    private static final int PORT = 63018;
    private final TradeService tradeService;
    private final TicketPrinterService ticketPrinterService;
    private static final Logger logger = LoggerFactory.getLogger(TradeController.class);

    public TradeController(TradeService tradeService, TicketPrinterService ticketPrinterService) {
        this.tradeService = tradeService;
        this.ticketPrinterService = ticketPrinterService;
    }

    @GetMapping("/")
    public List<TradeDto> getAll() {
        return tradeService.findAll();
    }

    @GetMapping("/{id}")
    public Optional<TradeDto> getById(@PathVariable(name = "id") final Long id) {
        return tradeService.findById(id);
    }

    @PostMapping("/")
    public TradeDto save(@RequestBody TradeDto entity) {
        return tradeService.save(entity);
    }

    /** Venta desde la caja: líneas {itemId, quantity}. Imprime y abre el cajón si toca. */
    @PostMapping("/sale")
    public TradeDto sale(@RequestBody SaleRequest sale, HttpServletRequest request) {
        PaymentMethod method = PaymentMethod.parse(sale.paymentMethod());
        TradeDto trade = tradeService.sell(sale.lines(), method);
        afterSale(trade, method, sale.print(), request);
        return trade;
    }

    /** Formato antiguo: un elemento por unidad vendida. Se mantiene por compatibilidad. */
    @Deprecated
    @PostMapping("/newTrade")
    public TradeDto saveList(@RequestBody List<ItemDto> items,
            @RequestParam(name = "print", defaultValue = "false") boolean print,
            @RequestParam(name = "paymentMethod", defaultValue = "CASH") String paymentMethod,
            HttpServletRequest request) {
        PaymentMethod method = PaymentMethod.parse(paymentMethod);
        TradeDto trade = tradeService.saveList(items, method);
        afterSale(trade, method, print, request);
        return trade;
    }

    /** Ticket y cajón (impresora del cliente). Un fallo aquí nunca afecta a la venta ya guardada. */
    private void afterSale(TradeDto trade, PaymentMethod method, boolean print, HttpServletRequest request) {
        String clientIp = clientIp(request);
        try {
            // Con tarjeta no entra dinero en el cajón: no se abre.
            boolean openDrawer = method == PaymentMethod.CASH;
            if (print) {
                ticketPrinterService.print(trade, clientIp, PORT, openDrawer);
            } else if (openDrawer) {
                ticketPrinterService.openCashDrawer(clientIp, PORT);
            }
        } catch (Exception e) {
            logger.warn("Impresora no disponible (la venta se ha guardado igualmente): {}", e.getMessage());
        }
    }

    private static String clientIp(HttpServletRequest request) {
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        } else {
            clientIp = clientIp.split(",")[0].trim(); // el primero es el cliente original
        }
        return clientIp;
    }

    /**
     * Imprime en la impresora de tickets el resumen del día (hoy si no se indica fecha).
     * mode=products: todos los productos vendidos; mode=categories: agrupados por categoría.
     * A diferencia del ticket de venta, aquí imprimir es el objetivo: si no hay impresora se avisa.
     */
    @PostMapping("/print-daily")
    public ResponseEntity<?> printDaily(
            @RequestParam(name = "mode", defaultValue = "products") String mode,
            @RequestParam(name = "date", required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            HttpServletRequest request) {
        if (!"products".equals(mode) && !"categories".equals(mode)) {
            return ResponseEntity.badRequest().body("mode debe ser 'products' o 'categories'");
        }
        DailyReportDto report = tradeService.dailyReport(date);
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();

        }
        try {
            ticketPrinterService.printDailyReport(report, "categories".equals(mode), clientIp, PORT);
        } catch (Exception e) {
            logger.warn("No se pudo imprimir el resumen del día: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE)
                    .body("No se pudo imprimir: " + e.getMessage());
        }
        return ResponseEntity.ok().build();
    }

    @GetMapping("/range")
    public List<TradeDto> getTradesInRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return tradeService.findTradesInRange(startDate, endDate);
    }

    /** Anula la venta (no la borra). Devuelve la venta con estado CANCELLED. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TradeDto> cancel(@PathVariable(name = "id") final Long id,
            @RequestParam(name = "reason", required = false) String reason) {
        try {
            return ResponseEntity.ok(tradeService.cancel(id, reason));
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Compatibilidad con el panel actual: DELETE ya no borra, anula.
     * Cuando el frontend use POST /{id}/cancel se puede retirar.
     */
    @Deprecated
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable(name = "id") final Long id) {
        try {
            tradeService.cancel(id, null);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{page}/{size}")
    public Page<TradeDto> getPage(@PathVariable("page") int page, @PathVariable("size") int size) {
        return tradeService.getPage(page, size);
    }
}