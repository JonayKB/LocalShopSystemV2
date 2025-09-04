package es.jonay.kb.shopsystem.model.entities;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.SortedMap;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.Table;

@Entity()
@Table(name = "Items")
public class Item {
    @Id
    private Long id;
    private String name;
    @ManyToOne
    private Category category;

    private String image;

    @Column(name = "stock", columnDefinition = "Integer default 0")
    private Integer stock = 0;

    @Column(name = "bareMinimun", columnDefinition = "Integer default 0")
    private Integer bareMinimun = 0;

    @Column(name = "ignoreStock", columnDefinition = "Boolean default false")
    private Boolean ignoreStock = false;

    @ElementCollection
    @MapKeyColumn(name = "date")
    @Column(name = "price")
    @CollectionTable(name = "price_history", joinColumns = @JoinColumn(name = "item_id"))
    private SortedMap<LocalDateTime, Double> priceHistory;

    @ElementCollection
    @MapKeyColumn(name = "date")
    @Column(name = "net")
    @CollectionTable(name = "net_history", joinColumns = @JoinColumn(name = "item_id"))
    private SortedMap<LocalDateTime, Double> netHistory;

    @ManyToMany(mappedBy = "items")
    private List<Trade> trades;

    public Item() {
    }

    public Item(Long id, String name, Category category, Integer stock, Integer bareMinimun,
            SortedMap<LocalDateTime, Double> netHistory,
            Boolean ignoreStock) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.stock = stock;
        this.bareMinimun = bareMinimun;
        this.netHistory = netHistory;
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

    public SortedMap<LocalDateTime, Double> getNetHistory() {
        return this.netHistory;
    }

    public void setNetHistory(SortedMap<LocalDateTime, Double> netHistory) {
        this.netHistory = netHistory;
    }

    public SortedMap<LocalDateTime, Double> getPriceHistory() {
        return this.priceHistory;
    }

    public void setPriceHistory(SortedMap<LocalDateTime, Double> priceHistory) {
        this.priceHistory = priceHistory;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Item)) {
            return false;
        }
        Item item = (Item) o;
        return id == item.id;
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
