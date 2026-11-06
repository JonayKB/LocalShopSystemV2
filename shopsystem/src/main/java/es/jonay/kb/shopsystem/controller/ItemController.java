package es.jonay.kb.shopsystem.controller;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.ItemDto;
import es.jonay.kb.shopsystem.api.exceptions.ConflictException;
import es.jonay.kb.shopsystem.api.mappers.ItemMapper;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.StockMovementType;
import es.jonay.kb.shopsystem.model.repository.ICategoryRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

@Controller
public class ItemController {
    /** Campos por los que se puede ordenar el listado (evita errores 500 con valores raros). */
    private static final Set<String> SORTABLE = Set.of("name", "id", "stock", "price", "cost", "bareMinimun");
    private static final int MAX_PAGE_SIZE = 200;

    private IItemRepository itemRepository;
    private ICategoryRepository categoryRepository;
    private ImageUsageGuard imageUsageGuard;
    private InventoryController inventory;
    private PricingController pricing;

    @Autowired
    public void setImageUsageGuard(ImageUsageGuard imageUsageGuard) {
        this.imageUsageGuard = imageUsageGuard;
    }

    @Autowired
    public void setIItemRepository(IItemRepository itemRepository) {
        this.itemRepository = itemRepository;
    }

    @Autowired
    public void setIICategoryRepository(ICategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Autowired
    public void setInventory(InventoryController inventory) {
        this.inventory = inventory;
    }

    @Autowired
    public void setPricing(PricingController pricing) {
        this.pricing = pricing;
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findAll() {
        return itemRepository.findAllWithCategory().stream().map(ItemMapper.INSTANCE::toItemDto).toList();
    }

    @Transactional(readOnly = true)
    public Optional<ItemDto> findById(Long id) {
        return itemRepository.findById(id).map(ItemMapper.INSTANCE::toItemDto);
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findByCategoryId(Long categoryId) {
        return itemRepository.findByCategoryId(categoryId).stream().map(ItemMapper.INSTANCE::toItemDto).toList();
    }

    @Transactional(readOnly = true)
    public List<ItemDto> findByTradeId(Long tradeId) {
        return itemRepository.findByTradesId(tradeId).stream().map(ItemMapper.INSTANCE::toItemDto).toList();
    }

    /**
     * Alta o edición (el panel usa el mismo formulario): si el código ya existe se edita
     * conservando su historial de precios y movimientos; si no, se crea.
     */
    @Transactional
    public ItemDto save(ItemDto itemDto) {
        if (itemDto.getId() != null && itemRepository.existsById(itemDto.getId())) {
            return update(itemDto);
        }
        validate(itemDto);
        imageUsageGuard.checkForItem(itemDto.getId(), null, itemDto.getImage());

        Item item = new Item();
        item.setId(itemDto.getId());
        applyFields(item, itemDto);
        item.setPrice(PricingController.money(itemDto.getPrice(), "El precio de venta"));
        item.setCost(PricingController.money(itemDto.getNet() == null ? java.math.BigDecimal.ZERO : itemDto.getNet(),
                "El coste"));
        item.setStock(0);
        item = itemRepository.save(item);
        pricing.initial(item, "Alta del producto");

        int initialStock = itemDto.getStock() == null ? 0 : itemDto.getStock();
        if (initialStock != 0) {
            Item locked = inventory.lock(item.getId());
            inventory.apply(locked, initialStock > 0 ? StockMovementType.OPENING_BALANCE : StockMovementType.ADJUSTMENT,
                    initialStock, null, null, "Stock inicial al dar de alta el producto");
            item = locked;
        }
        return ItemMapper.INSTANCE.toItemDto(item);
    }

    /** Edita un producto existente. Precio/coste y stock dejan rastro en sus historiales. */
    @Transactional
    public ItemDto update(ItemDto itemDto) {
        if (itemDto.getId() == null) {
            throw new IllegalArgumentException("Falta el código del producto");
        }
        validate(itemDto);
        Item item = inventory.lock(itemDto.getId());
        imageUsageGuard.checkForItem(item.getId(), item.getImage(), itemDto.getImage());
        applyFields(item, itemDto);
        pricing.change(item, itemDto.getPrice(),
                itemDto.getNet() == null ? item.getCost() : itemDto.getNet(), "Edición de la ficha del producto");
        if (itemDto.getStock() != null) {
            String note = itemDto.getStockNote() != null && !itemDto.getStockNote().isBlank() ? itemDto.getStockNote()
                    : "Ajuste manual desde la ficha del producto";
            if (itemDto.getOriginalStock() != null) {
                inventory.apply(item, StockMovementType.ADJUSTMENT, itemDto.getStock() - itemDto.getOriginalStock(),
                        null, null, note);
            } else {
                inventory.adjustTo(item, itemDto.getStock(), note);
            }
        }
        return ItemMapper.INSTANCE.toItemDto(itemRepository.save(item));
    }

    private void applyFields(Item item, ItemDto dto) {
        item.setName(dto.getName().trim());
        item.setCategory(categoryOf(dto.getCategoryId()));
        item.setImage(dto.getImage() == null || dto.getImage().isBlank() ? null : dto.getImage().trim());
        item.setBareMinimun(dto.getBareMinimun() == null ? 0 : dto.getBareMinimun());
        item.setIgnoreStock(Boolean.TRUE.equals(dto.getIgnoreStock()));
    }

    private Category categoryOf(Long categoryId) {
        if (categoryId == null) {
            return null;
        }
        return categoryRepository.findById(categoryId)
                .orElseThrow(() -> new IllegalArgumentException("La categoría " + categoryId + " no existe"));
    }

    private static void validate(ItemDto dto) {
        if (dto.getId() == null || dto.getId() < 0) {
            throw new IllegalArgumentException("El código del producto no es válido");
        }
        if (dto.getName() == null || dto.getName().isBlank()) {
            throw new IllegalArgumentException("El nombre del producto es obligatorio");
        }
        if (dto.getBareMinimun() != null && dto.getBareMinimun() < 0) {
            throw new IllegalArgumentException("El stock mínimo no puede ser negativo");
        }
        PricingController.money(dto.getPrice(), "El precio de venta");
        if (dto.getNet() != null) {
            PricingController.money(dto.getNet(), "El coste");
        }
    }

    @Transactional
    public void deleteById(Long id) {
        if (!itemRepository.existsById(id)) {
            throw new NoSuchElementException("Producto no encontrado: " + id);
        }
        try {
            itemRepository.deleteById(id);
            itemRepository.flush();
        } catch (DataIntegrityViolationException e) {
            throw new ConflictException(
                    "No se puede borrar: el producto aparece en ventas antiguas. Déjalo sin stock o cámbiale el nombre.");
        }
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> findByNameContainingIgnoreCase(String name, Integer page, Integer size, String sortBy,
            boolean ascending) {
        return itemRepository.findByNameContainingIgnoreCase(name == null ? "" : name, pageable(page, size, sortBy, ascending))
                .map(ItemMapper.INSTANCE::toItemDto);
    }

    @Transactional(readOnly = true)
    public Page<ItemDto> findByCategoryId(String name, int page, int size, String sortBy, boolean ascending,
            Long categoryId) {
        Pageable pageable = pageable(page, size, sortBy, ascending);
        String n = name == null ? "" : name;
        if (categoryId == null) {
            return itemRepository.findByNameContainingIgnoreCase(n, pageable).map(ItemMapper.INSTANCE::toItemDto);
        }
        return itemRepository.findByCategoryIdAndNameContainingIgnoreCase(categoryId, n, pageable)
                .map(ItemMapper.INSTANCE::toItemDto);
    }

    private static Pageable pageable(Integer page, Integer size, String sortBy, boolean ascending) {
        String field = sortBy == null || sortBy.isBlank() ? "name" : sortBy;
        if (!SORTABLE.contains(field)) {
            throw new IllegalArgumentException("No se puede ordenar por " + sortBy);
        }
        int p = page == null ? 0 : Math.max(0, page);
        int s = size == null ? 20 : Math.max(1, Math.min(size, MAX_PAGE_SIZE));
        Sort sort = ascending ? Sort.by(field).ascending() : Sort.by(field).descending();
        return PageRequest.of(p, s, sort.and(Sort.by("id")));
    }
}
