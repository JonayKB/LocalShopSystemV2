package es.jonay.kb.shopsystem.services;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.MediaType;
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
    TradeController tradeController;
    TicketPrinterService ticketPrinterService;

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

        TradeDto trade = tradeController.saveList(items);
        
        if (print) {
            String clientIp = request.getHeader("X-Forwarded-For");
            if (clientIp == null || clientIp.isEmpty()) {
                clientIp = request.getRemoteAddr();
            }
            ticketPrinterService.print(trade, clientIp, 9100); // Usa la IP pública del cliente
        }
        return ResponseEntity.ok(trade);
    }

    @GetMapping("/range")
    public List<TradeDto> getTradesInRange(
            @RequestParam("startDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime startDate,
            @RequestParam("endDate") @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime endDate) {
        return tradeController.findTradesInRange(startDate, endDate);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable(name = "id") final Long id) {
        try {
            tradeController.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{page}/{size}")
    public Page<TradeDto> getPage(@PathVariable("page") int page, @PathVariable("size") int size) {
        return tradeController.getPage(page, size);
    }
}
