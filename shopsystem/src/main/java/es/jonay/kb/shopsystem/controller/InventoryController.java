package es.jonay.kb.shopsystem.controller;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.NoSuchElementException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import es.jonay.kb.shopsystem.api.dto.StockMovementDto;
import es.jonay.kb.shopsystem.api.dto.StockMovementRequest;
import es.jonay.kb.shopsystem.model.entities.Item;
import es.jonay.kb.shopsystem.model.entities.StockMovement;
import es.jonay.kb.shopsystem.model.entities.StockMovementType;
import es.jonay.kb.shopsystem.model.repository.IItemRepository;
import es.jonay.kb.shopsystem.model.repository.IStockMovementRepository;

/**
 * Único punto por el que cambia el stock. Cada cambio:
 * <ul>
 * <li>bloquea la fila del producto (SELECT ... FOR UPDATE) hasta el final de la transacción,</li>
 * <li>suma o resta en el propio producto (stock en caché para listados rápidos) y</li>
 * <li>deja un {@link StockMovement} inmutable con el saldo resultante.</li>
 * </ul>
 * Así la suma de movimientos de un producto es siempre igual a su stock.
 */
@Service
public class InventoryController {

    private static final int MAX_NOTE = 255;

    private final IItemRepository items;
    private final IStockMovementRepository movements;
    private final PricingController pricing;
    private Clock clock = Clock.systemUTC();

    public InventoryController(IItemRepository items, IStockMovementRepository movements, PricingController pricing) {
        this.items = items;
        this.movements = movements;
        this.pricing = pricing;
    }

    @Autowired(required = false)
    void setClock(Clock clock) {
        this.clock = clock;
    }

    /** Bloquea el producto para modificar su stock en esta transacción. */
    @Transactional(propagation = Propagation.MANDATORY)
    public Item lock(Long itemId) {
        if (itemId == null) {
            throw new IllegalArgumentException("Falta el código del producto");
        }
        return items.findByIdForUpdate(itemId)
                .orElseThrow(() -> new NoSuchElementException("Producto no encontrado: " + itemId));
    }

    /**
     * Aplica un movimiento a un producto ya bloqueado con {@link #lock(Long)}.
     *
     * @param signedQuantity unidades con signo (+ entra, - sale). 0 no hace nada.
     */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement apply(Item locked, StockMovementType type, int signedQuantity, BigDecimal unitCost,
            Long tradeId, String note) {
        if (signedQuantity == 0) {
            return null;
        }
        if (type.sign() != 0 && Integer.signum(signedQuantity) != type.sign()) {
            throw new IllegalArgumentException(type.label() + ": la cantidad debe ser "
                    + (type.sign() > 0 ? "positiva" : "negativa"));
        }
        int current = locked.getStock() == null ? 0 : locked.getStock();
        int after = Math.addExact(current, signedQuantity);
        locked.setStock(after);
        items.save(locked);
        return movements.save(new StockMovement(locked.getId(), locked.getName(), type, signedQuantity, after,
                unitCost != null ? unitCost : locked.getCost(), tradeId, trim(note), currentUser(), clock.instant()));
    }

    /** Deja el stock de un producto bloqueado en {@code newStock} con un ajuste (si cambia). */
    @Transactional(propagation = Propagation.MANDATORY)
    public StockMovement adjustTo(Item locked, int newStock, String note) {
        int current = locked.getStock() == null ? 0 : locked.getStock();
        return apply(locked, StockMovementType.ADJUSTMENT, newStock - current, null, null, note);
    }

    /**
     * Registra a mano entradas, devoluciones, mermas o ajustes de varios productos en una sola
     * operación (todo o nada). Los productos se bloquean ordenados por id para evitar bloqueos
     * cruzados con otras cajas.
     */
    @Transactional
    public List<StockMovementDto> register(StockMovementRequest request) {
        StockMovementType type = parseManualType(request == null ? null : request.type());
        if (request.lines() == null || request.lines().isEmpty()) {
            throw new IllegalArgumentException("No hay productos en el movimiento");
        }
        List<StockMovementRequest.Line> lines = new ArrayList<>(request.lines());
        lines.sort(Comparator.comparing(StockMovementRequest.Line::itemId,
                Comparator.nullsFirst(Comparator.naturalOrder())));

        List<StockMovementDto> result = new ArrayList<>();
        for (StockMovementRequest.Line line : lines) {
            Item item = lock(line.itemId());
            int signed = signedQuantity(type, line, item);
            BigDecimal unitCost = line.unitCost();
            if (unitCost != null && unitCost.signum() < 0) {
                throw new IllegalArgumentException("El coste no puede ser negativo");
            }
            if (type == StockMovementType.RESTOCK && unitCost != null) {
                // El coste de la última entrada pasa a ser el coste del producto (queda en el historial).
                pricing.change(item, item.getPrice(), unitCost, "Coste de la entrada de mercancía");
            }
            StockMovement m = apply(item, type, signed, unitCost, null, request.note());
            if (m != null) {
                result.add(toDto(m));
            }
        }
        return result;
    }

    private static int signedQuantity(StockMovementType type, StockMovementRequest.Line line, Item item) {
        if (type == StockMovementType.ADJUSTMENT) {
            if (line.countedStock() != null) {
                if (line.countedStock() < 0) {
                    throw new IllegalArgumentException("El recuento no puede ser negativo");
                }
                int current = item.getStock() == null ? 0 : item.getStock();
                return line.countedStock() - current;
            }
            if (line.quantity() == null) {
                throw new IllegalArgumentException("Indica el recuento o la cantidad del ajuste");
            }
            return line.quantity();
        }
        if (line.quantity() == null || line.quantity() <= 0) {
            throw new IllegalArgumentException("La cantidad de " + item.getName() + " debe ser mayor que 0");
        }
        return type.sign() * line.quantity();
    }

    private static StockMovementType parseManualType(String raw) {
        StockMovementType type;
        try {
            type = StockMovementType.valueOf(raw == null ? "" : raw.trim().toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Tipo de movimiento no válido: " + raw);
        }
        if (!type.isManual()) {
            throw new IllegalArgumentException("El tipo " + type.label() + " no se puede registrar a mano");
        }
        return type;
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> forItem(Long itemId, int page, int size) {
        return movements.findByItemIdOrderByCreatedAtDescIdDesc(itemId, PageRequest.of(page, clampSize(size)))
                .map(InventoryController::toDto);
    }

    @Transactional(readOnly = true)
    public Page<StockMovementDto> search(Long itemId, String type, Instant from, Instant to, int page, int size) {
        StockMovementType t = null;
        if (type != null && !type.isBlank()) {
            try {
                t = StockMovementType.valueOf(type.trim().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new IllegalArgumentException("Tipo de movimiento no válido: " + type);
            }
        }
        return movements.search(itemId, t, from, to, PageRequest.of(page, clampSize(size)))
                .map(InventoryController::toDto);
    }

    public static StockMovementDto toDto(StockMovement m) {
        return new StockMovementDto(m.getId(), m.getItemId(), m.getItemName(), m.getType().name(),
                m.getType().label(), m.getQuantity(), m.getStockAfter(), m.getUnitCost(), m.getTradeId(),
                m.getNote(), m.getCreatedBy(), m.getCreatedAt());
    }

    private static int clampSize(int size) {
        return Math.max(1, Math.min(size, 200));
    }

    private static String trim(String note) {
        if (note == null || note.isBlank()) {
            return null;
        }
        String n = note.trim();
        return n.length() > MAX_NOTE ? n.substring(0, MAX_NOTE) : n;
    }

    static String currentUser() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        return auth != null && auth.isAuthenticated() && auth.getName() != null ? auth.getName() : "sistema";
    }
}
