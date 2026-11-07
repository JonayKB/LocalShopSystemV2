package es.jonay.kb.shopsystem.model.entities;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Libro de movimientos de stock (inmutable). quantity lleva signo (+ entra, - sale) y
 * stockAfter es el stock del producto justo después del movimiento.
 */
@Entity
@Table(name = "stock_movements")
public class StockMovement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    /** Nombre del producto en el momento del movimiento. */
    @Column(name = "item_name")
    private String itemName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private StockMovementType type;

    @Column(nullable = false)
    private Integer quantity;

    @Column(name = "stock_after", nullable = false)
    private Integer stockAfter;

    @Column(name = "unit_cost", precision = 10, scale = 2)
    private BigDecimal unitCost;

    @Column(name = "trade_id")
    private Long tradeId;

    private String note;

    @Column(name = "created_by")
    private String createdBy;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    protected StockMovement() {
    }

    public StockMovement(Long itemId, String itemName, StockMovementType type, int quantity, int stockAfter,
            BigDecimal unitCost, Long tradeId, String note, String createdBy, Instant createdAt) {
        this.itemId = itemId;
        this.itemName = itemName;
        this.type = type;
        this.quantity = quantity;
        this.stockAfter = stockAfter;
        this.unitCost = unitCost;
        this.tradeId = tradeId;
        this.note = note;
        this.createdBy = createdBy;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public Long getItemId() {
        return itemId;
    }

    public String getItemName() {
        return itemName;
    }

    public StockMovementType getType() {
        return type;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public Integer getStockAfter() {
        return stockAfter;
    }

    public BigDecimal getUnitCost() {
        return unitCost;
    }

    public Long getTradeId() {
        return tradeId;
    }

    public String getNote() {
        return note;
    }

    public String getCreatedBy() {
        return createdBy;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }
}
