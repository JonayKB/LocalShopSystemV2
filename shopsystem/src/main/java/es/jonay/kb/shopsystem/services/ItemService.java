package es.jonay.kb.shopsystem.services;

import java.util.List;
import java.util.Optional;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.mappers.ItemMapper;
import es.jonay.kb.shopsystem.controller.ItemController;

@RestController
@RequestMapping("items")
@CrossOrigin
public class ItemService {
    ItemController iItemController;

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
        try {
            iItemController.deleteById(id);
            return ResponseEntity.noContent().build();
        } catch (RuntimeException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/stock")
    public ItemDto addStock(@PathVariable(name = "id") final Long id, @RequestBody Integer amount) {
        return ItemMapper.INSTANCE.toItemDto(iItemController.addStock(id, amount));
    }

    @DeleteMapping("/{id}/stock")
    public ItemDto removeStock(@PathVariable(name = "id") final Long id, @RequestBody Integer amount) {
        return ItemMapper.INSTANCE.toItemDto(iItemController.removeStock(id, amount));
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
