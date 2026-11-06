package es.jonay.kb.shopsystem.services;

import org.springframework.web.bind.annotation.RequestParam;
import es.jonay.kb.shopsystem.api.dto.PriceChangeDto;
import es.jonay.kb.shopsystem.api.dto.StockMovementDto;
import es.jonay.kb.shopsystem.controller.InventoryController;
import es.jonay.kb.shopsystem.controller.PricingController;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.exceptions.ImageInUseException;
import es.jonay.kb.shopsystem.api.mappers.ItemMapper;
import es.jonay.kb.shopsystem.controller.ItemController;

@RestController
@RequestMapping("items")
public class ItemService {
    ItemController iItemController;
    @Autowired
    PricingController pricing;
    @Autowired
    InventoryController inventory;

    public ItemController getIItemController() {
        return this.iItemController;
    }

    @Autowired
    public void setIItemController(ItemController iItemController) {
        this.iItemController = iItemController;
    }

    @GetMapping("/")
    public List<ItemDto> getAll() {
        return iItemController.findAll();
    }

    @GetMapping("/{id}")
    public Optional<ItemDto> getById(@PathVariable(name = "id") final Long id) {
        return iItemController.findById(id);
    }

    @GetMapping("/categories/{categoryId}")
    public List<ItemDto> getByCategoryId(@PathVariable(name = "categoryId") final Long categoryId) {
        return iItemController.findByCategoryId(categoryId);
    }

    @GetMapping("/trades/{tradeID}")
    public List<ItemDto> getByTradeId(@PathVariable(name = "tradeID") final Long tradeId) {
        return iItemController.findByTradeId(tradeId);
    }

    @PostMapping("/")
    public ItemDto save(@RequestBody ItemDto entity) {
        return iItemController.save(entity);
    }

    @PutMapping("/")
    public ItemDto update(@RequestBody ItemDto entity) {
        return iItemController.update(entity);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteById(@PathVariable(name = "id") final Long id) {
        iItemController.deleteById(id);
        return ResponseEntity.noContent().build();
    }

    /** Historial de precios de venta y costes, del cambio más reciente al más antiguo. */
    @GetMapping("/{id}/price-history")
    public List<PriceChangeDto> priceHistory(@PathVariable(name = "id") final Long id) {
        return pricing.historyOf(id);
    }

    /** Movimientos de stock del producto, del más reciente al más antiguo. */
    @GetMapping("/{id}/stock-movements")
    public Page<StockMovementDto> stockMovements(@PathVariable(name = "id") final Long id,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size) {
        return inventory.forItem(id, page, size);
    }

    @GetMapping("/{page}/{size}")
    public Page<ItemDto> findByNameContainingIgnoreCase(String name,
            @PathVariable(name = "page") int page,
            @PathVariable(name = "size") int size,
            String sortBy,
            boolean ascending) {
        return iItemController.findByNameContainingIgnoreCase(name, page, size, sortBy, ascending);
    }

    @GetMapping("{categoryId}/{page}/{size}")
    public Page<ItemDto> findByCategoryId(@PathVariable(name = "categoryId") Long categoryId,
            @PathVariable(name = "page") int page,
            @PathVariable(name = "size") int size,
            String sortBy,
            boolean ascending,
            String name) {
        return iItemController.findByCategoryId(name, page, size, sortBy, ascending, categoryId);
    }
}
