package es.jonay.kb.shopsystem.api.dto;

import java.math.BigDecimal;
import java.util.Objects;

/**
 * Producto en la API. price = precio de venta (IGIC incluido); net = coste (sin IGIC).
 * stockNote: motivo opcional cuando al editar la ficha se cambia el stock a mano.
 */
public class ItemDto {
    private Long id;
    private String name;
    private BigDecimal price = BigDecimal.ZERO;
    private Long categoryId;
    private String image;
    private Integer stock;
    private Integer bareMinimun;
    private BigDecimal net = BigDecimal.ZERO;
    private Boolean ignoreStock;
    private String stockNote;
    /**
     * Stock que mostraba el formulario al abrirse. Si se envía, al editar solo se aplica la
     * diferencia (stock - originalStock): una venta hecha mientras tanto no se "deshace".
     */
    private Integer originalStock;

    public ItemDto() {
    }

    public ItemDto(Long id, String name, BigDecimal price, Long categoryId, Integer stock, Integer bareMinimun,
            Boolean ignoreStock, BigDecimal net) {
        this.id = id;
        this.name = name;
        this.price = price;
        this.categoryId = categoryId;
        this.stock = stock;
        this.bareMinimun = bareMinimun;
        this.ignoreStock = ignoreStock;
        this.net = net;
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

    public BigDecimal getPrice() {
        return this.price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public Long getCategoryId() {
        return this.categoryId;
    }

    public void setCategoryId(Long categoryId) {
        this.categoryId = categoryId;
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

    public BigDecimal getNet() {
        return this.net;
    }

    public void setNet(BigDecimal net) {
        this.net = net;
    }

    public String getStockNote() {
        return stockNote;
    }

    public void setStockNote(String stockNote) {
        this.stockNote = stockNote;
    }

    public Integer getOriginalStock() {
        return originalStock;
    }

    public void setOriginalStock(Integer originalStock) {
        this.originalStock = originalStock;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof ItemDto)) {
            return false;
        }
        ItemDto itemDto = (ItemDto) o;
        return Objects.equals(id, itemDto.id);
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
                ", price='" + getPrice() + "'" +
                ", categoryId='" + getCategoryId() + "'" +
                ", stock='" + getStock() + "'" +
                "}";
    }

}
