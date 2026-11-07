package es.jonay.kb.shopsystem.model.entities;

import java.math.BigDecimal;
import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Una fila por cada cambio de precio de venta o de coste de un producto (historial
 * inmutable). El precio vigente en una fecha es el de la última fila con validFrom <= fecha.
 */
@Entity
@Table(name = "item_price_history")
public class ItemPriceChange {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "item_id", nullable = false)
    private Long itemId;

    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @Column(name = "cost_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal cost;

    @Column(name = "valid_from", nullable = false)
    private Instant validFrom;

    @Column(name = "changed_by")
    private String changedBy;

    private String reason;

    protected ItemPriceChange() {
    }

    public ItemPriceChange(Long itemId, BigDecimal price, BigDecimal cost, Instant validFrom, String changedBy,
            String reason) {
        this.itemId = itemId;
        this.price = price;
        this.cost = cost;
        this.validFrom = validFrom;
        this.changedBy = changedBy;
        this.reason = reason;
    }

    public Long getId() {
        return id;
    }

    public Long getItemId() {
        return itemId;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public Instant getValidFrom() {
        return validFrom;
    }

    public String getChangedBy() {
        return changedBy;
    }

    public String getReason() {
        return reason;
    }
}
