package es.jonay.kb.shopsystem.controller;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.PriceChangeDto;
import es.jonay.kb.shopsystem.model.entities.Category;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.ItemPriceChange;
import es.jonay.kb.shopsystem.model.repository.IItemPriceHistoryRepository;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;

/**
 * Precios de venta y costes. Cada cambio deja una fila en el historial con fecha, usuario y
 * motivo; las ventas ya emitidas no cambian porque sus líneas guardan su propio precio.
 */
@Service
public class PricingController {

    static final int SCALE = 2;
    private static final BigDecimal HUNDRED = new BigDecimal("100");

    private final IItemPriceHistoryRepository history;
    private final IItemRepository items;
    private final BigDecimal defaultIgicRate;
    private Clock clock = Clock.systemUTC();

    public PricingController(IItemPriceHistoryRepository history, IItemRepository items,
            @Value("${shop.tax.default-igic-rate:7.00}") BigDecimal defaultIgicRate) {
        this.history = history;
        this.items = items;
        this.defaultIgicRate = defaultIgicRate;
    }

    @Autowired(required = false)
    void setClock(Clock clock) {
        this.clock = clock;
    }

    /** Normaliza un importe: 2 decimales, no negativo. */
    public static BigDecimal money(BigDecimal value, String field) {
        if (value == null) {
            throw new IllegalArgumentException("Falta " + field);
        }
        if (value.signum() < 0) {
            throw new IllegalArgumentException(field + " no puede ser negativo");
        }
        if (value.compareTo(new BigDecimal("99999999.99")) > 0) {
            throw new IllegalArgumentException(field + " es demasiado alto");
        }
        return value.setScale(SCALE, RoundingMode.HALF_UP);
    }

    /**
     * Cambia precio y/o coste del producto si alguno es distinto, y lo anota en el historial.
     * Devuelve true si hubo cambio.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public boolean change(Item item, BigDecimal newPrice, BigDecimal newCost, String reason) {
        BigDecimal price = money(newPrice, "El precio de venta");
        BigDecimal cost = money(newCost, "El coste");
        boolean samePrice = item.getPrice() != null && item.getPrice().compareTo(price) == 0;
        boolean sameCost = item.getCost() != null && item.getCost().compareTo(cost) == 0;
        if (samePrice && sameCost) {
            return false;
        }
        item.setPrice(price);
        item.setCost(cost);
        items.save(item);
        history.save(new ItemPriceChange(item.getId(), price, cost, clock.instant(),
                InventoryController.currentUser(), reason));
        return true;
    }

    /** Primera fila del historial de un producto recién creado. */
    @Transactional(propagation = Propagation.MANDATORY)
    public void initial(Item item, String reason) {
        history.save(new ItemPriceChange(item.getId(), item.getPrice(), item.getCost(), clock.instant(),
                InventoryController.currentUser(), reason));
    }

    @Transactional(readOnly = true)
    public List<PriceChangeDto> historyOf(Long itemId) {
        BigDecimal rate = items.findById(itemId).map(Item::getCategory).map(this::igicRateOf)
                .orElse(defaultIgicRate);
        return history.findByItemIdOrderByValidFromDescIdDesc(itemId).stream()
                .map(h -> new PriceChangeDto(h.getId(), h.getPrice(), h.getCost(), marginPct(h.getPrice(), h.getCost(), rate),
                        h.getValidFrom(), h.getChangedBy(), h.getReason()))
                .toList();
    }

    public BigDecimal igicRateOf(Category category) {
        if (category != null && category.getIgicRate() != null) {
            return category.getIgicRate();
        }
        return defaultIgicRate;
    }

    /** Margen (%) sobre el precio sin IGIC. Null si no hay coste o el precio es 0. */
    public static BigDecimal marginPct(BigDecimal priceWithTax, BigDecimal cost, BigDecimal ratePct) {
        if (priceWithTax == null || cost == null || cost.signum() == 0 || priceWithTax.signum() == 0) {
            return null;
        }
        BigDecimal base = priceWithTax.divide(BigDecimal.ONE.add(ratePct.divide(HUNDRED, 6, RoundingMode.HALF_UP)),
                6, RoundingMode.HALF_UP);
        return base.subtract(cost).multiply(HUNDRED).divide(base, 1, RoundingMode.HALF_UP);
    }
}
