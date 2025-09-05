package es.jonay.kb.shopsystem.services;

import java.util.HashMap;
import java.util.Map;
import java.util.logging.Logger;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.controller.AuthController;
import es.jonay.kb.shopsystem.controller.ItemController;
import es.jonay.kb.shopsystem.model.entities.Item;
import io.swagger.v3.oas.annotations.tags.Tag;

@RestController
@RequestMapping("stock")
@CrossOrigin
@Tag(name = "STOCK", description = "Stock management endpoints")
public class StockService {

    private final ItemController itemController;
    Logger logger = Logger.getLogger(StockService.class.getName());

    public StockService(ItemController itemController) {
        this.itemController = itemController;
    }

    @PostMapping("/")
    public ResponseEntity<String> addItem(@RequestBody Map<Long, Integer> itemsStock) {
        try {
            Map<Long,Integer> result = new HashMap<>();
            for (Map.Entry<Long, Integer> entry : itemsStock.entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();
                Item item = itemController.addStock(itemId, quantity);
                result.put(itemId, item.getStock());
            }
            return ResponseEntity.ok("Items stock added successfully: " + result.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

    @DeleteMapping("/")
    public ResponseEntity<String> removeItem(@RequestBody Map<Long, Integer> itemsStock) {
        try {
            Map<Long,Integer> result = new HashMap<>();
            for (Map.Entry<Long, Integer> entry : itemsStock.entrySet()) {
                Long itemId = entry.getKey();
                Integer quantity = entry.getValue();
                Item item = itemController.removeStock(itemId, quantity);
                result.put(itemId, item.getStock());
            }
            return ResponseEntity.ok("Items stock removed successfully: " + result.toString());
        } catch (Exception e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        }
    }

}
