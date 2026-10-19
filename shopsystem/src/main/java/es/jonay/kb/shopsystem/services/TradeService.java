package es.jonay.kb.shopsystem.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.logging.Logger;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.dto.TradeDto;
import es.jonay.kb.shopsystem.api.services.TicketPrinterService;
import es.jonay.kb.shopsystem.controller.TradeController;
import jakarta.servlet.http.HttpServletRequest;

@RestController
@RequestMapping("trade")
@CrossOrigin
public class TradeService {
    private static final int PORT = 63018;
    TradeController tradeController;
    TicketPrinterService ticketPrinterService;
    Logger logger = Logger.getLogger(TradeService.class.getName());

    public TradeController getTradeController() {
        return this.tradeController;
    }

    @Autowired
    public void setTradeController(TradeController tradeController) {
        this.tradeController = tradeController;
    }

    @Autowired
    public void setTicketPrinterService(TicketPrinterService ticketPrinterService) {
        this.ticketPrinterService = ticketPrinterService;
    }

    @GetMapping("/")
    public List<TradeDto> getAll() {
        return tradeController.findAll();
    }

    @GetMapping("/{id}")
    public Optional<TradeDto> getById(@PathVariable(name = "id") final Long id) {
        return tradeController.findById(id);
    }

    @PostMapping("/")
    public TradeDto save(@RequestBody TradeDto entity) {
        return tradeController.save(entity);
    }

    @PostMapping("/newTrade")
    public ResponseEntity<?> saveList(@RequestBody List<ItemDto> items,
            @RequestParam(name = "print", defaultValue = "false") boolean print,
            HttpServletRequest request) throws Exception {

        TradeDto trade;
        try {
            trade = tradeController.saveList(items);
        } catch (IllegalArgumentException | NoSuchElementException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
        String clientIp = request.getHeader("X-Forwarded-For");
        if (clientIp == null || clientIp.isEmpty()) {
            clientIp = request.getRemoteAddr();
        }
        System.out.println("Client IP: " + clientIp);
        try {
            if (print) {
                ticketPrinterService.print(trade, clientIp, PORT); // Usa la IP pública del cliente
            } else {
                ticketPrinterService.openCashDrawer(clientIp, PORT);
            }
        } catch (Exception e) {
            // La venta ya está guardada: un fallo de la impresora nunca debe afectarla.
            logger.warning("Impresora no disponible (la venta se ha guardado igualmente): " + e.getMessage());
        }

        return ResponseEntity.ok(trade);
    }

    @GetMapping("/range")
    public List<TradeDto> getTradesInRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return tradeController.findTradesInRange(startDate, endDate);
    }

    /** Anula la venta (no la borra). Devuelve la venta con estado CANCELLED. */
    @PostMapping("/{id}/cancel")
    public ResponseEntity<TradeDto> cancel(@PathVariable(name = "id") final Long id,
            @RequestParam(name = "reason", required = false) String reason) {
        try {
            return ResponseEntity.ok(tradeController.cancel(id, reason));
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
            tradeController.cancel(id, null);
            return ResponseEntity.noContent().build();
        } catch (NoSuchElementException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{page}/{size}")
    public Page<TradeDto> getPage(@PathVariable("page") int page, @PathVariable("size") int size) {
        return tradeController.getPage(page, size);
    }
}