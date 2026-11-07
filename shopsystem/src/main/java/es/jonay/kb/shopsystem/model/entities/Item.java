package es.jonay.kb.shopsystem.model.entities;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

/**
 * Producto. El id es el código de barras. El precio y el coste actuales viven aquí (DECIMAL);
 * cada cambio queda registrado en {@link ItemPriceChange} y cada variación de stock en
 * {@link StockMovement}. El stock solo debe cambiarse a través del servicio de inventario.
 */
@Entity
@Table(name = "items")
public class Item {
    @Id
    private Long id;
    private String name;
    @ManyToOne
    private Category category;

    private String image;

    @Column(name = "stock", columnDefinition = "int default 0")
    private Integer stock = 0;

    @Column(name = "bare_minimun", columnDefinition = "int default 0")
    private Integer bareMinimun = 0;

    @Column(name = "ignore_stock", columnDefinition = "tinyint(1) default 0")
    private Boolean ignoreStock = false;

    /** Precio de venta actual, IGIC incluido. */
    @Column(name = "sale_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal price = BigDecimal.ZERO;

    /** Coste de compra actual (sin IGIC). 0 = desconocido. */
    @Column(name = "cost_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal cost = BigDecimal.ZERO;

    /** Solo ventas anteriores a las líneas de venta (tabla trades_items). */
    @ManyToMany(mappedBy = "items")
    private List<Trade> trades;

    public Item() {
    }

    public Item(Long id, String name, Category category, Integer stock, Integer bareMinimun, Boolean ignoreStock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.stock = stock;
        this.bareMinimun = bareMinimun;
        this.ignoreStock = ignoreStock;
    }

    public Category getCategory() {
        return this.category;
    }

    public void setCategory(Category category) {
        this.category = category;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return this.name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getImage() {
        return this.image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    public Integer getStock() {
        return this.stock;
    }

    public void setStock(Integer stock) {
        this.stock = stock;
    }

    public Integer getBareMinimun() {
        return this.bareMinimun;
    }

    public void setBareMinimun(Integer bareMinimun) {
        this.bareMinimun = bareMinimun;
    }

    public Boolean getIgnoreStock() {
        return this.ignoreStock;
    }

    public void setIgnoreStock(Boolean ignoreStock) {
        this.ignoreStock = ignoreStock;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public BigDecimal getCost() {
        return cost;
    }

    public void setCost(BigDecimal cost) {
        this.cost = cost;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Item)) {
            return false;
        }
        Item item = (Item) o;
        return Objects.equals(id, item.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "{" +
                " id='" + getId() + "'" +
                ", name='" + getName() + "'" +
                ", stock='" + getStock() + "'" +
                "}";
    }

}
