package es.jonay.kb.shopsystem.model.entities;

import java.math.BigDecimal;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Línea de una venta. Es una "foto" inmutable de lo vendido: nombre, precio y
 * tipo de IGIC se copian aquí en el momento de la venta, de modo que cambiar
 * después el producto o su precio no altera ventas ya emitidas.
 */
@Entity
@Table(name = "trade_lines")
public class TradeLine {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    private Trade trade;

    /** Referencia informativa al producto (puede cambiar o desaparecer). */
    @Column(name = "item_id")
    private Long itemId;

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private Integer quantity;

    /** Precio unitario final (IGIC incluido). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    /** Tipo de IGIC aplicado, en porcentaje (p. ej. 7.00). */
    @Column(nullable = false, precision = 5, scale = 2)
    private BigDecimal taxRate;

    /** Base imponible de la línea. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxBase;

    /** Cuota de IGIC de la línea. */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal taxAmount;

    /** Importe total de la línea (base + cuota). */
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal;

    public TradeLine() {
    }

    public Long getId() {
        return id;
    }

    public Trade getTrade() {
        return trade;
    }

    public void setTrade(Trade trade) {
        this.trade = trade;
    }

    public Long getItemId() {
        return itemId;
    }

    public void setItemId(Long itemId) {
        this.itemId = itemId;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getQuantity() {
        return quantity;
    }

    public void setQuantity(Integer quantity) {
        this.quantity = quantity;
    }

    public BigDecimal getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(BigDecimal unitPrice) {
        this.unitPrice = unitPrice;
    }

    public BigDecimal getTaxRate() {
        return taxRate;
    }

    public void setTaxRate(BigDecimal taxRate) {
        this.taxRate = taxRate;
    }

    public BigDecimal getTaxBase() {
        return taxBase;
    }

    public void setTaxBase(BigDecimal taxBase) {
        this.taxBase = taxBase;
    }

    public BigDecimal getTaxAmount() {
        return taxAmount;
    }

    public void setTaxAmount(BigDecimal taxAmount) {
        this.taxAmount = taxAmount;
    }

    public BigDecimal getLineTotal() {
        return lineTotal;
    }

    public void setLineTotal(BigDecimal lineTotal) {
        this.lineTotal = lineTotal;
    }
}
