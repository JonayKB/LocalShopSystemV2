package es.jonay.kb.shopsystem.model.entities;

import java.util.List;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

@Entity()
@Table(name = "Items")
public class Item {
    @Id
    private Long id;
    private String name;
    private Double price;
    @ManyToOne
    private Category category;

    private String image;

    @Column(name = "stock", columnDefinition = "Integer default 0")
    private Integer stock = 0;

    @Column(name = "bareMinimun", columnDefinition = "Integer default 0")
    private Integer bareMinimun = 0;

    @Column(name = "net", columnDefinition = "Decimal(10,2) default 0")
    private Double net = 0.0;
    @Column(name = "ignoreStock", columnDefinition = "Boolean default false")
    private Boolean ignoreStock = false;

    @ManyToMany(mappedBy = "items")
    private List<Trade> trades;

    public Item() {
    }

    public Item(Long id, String name, Double price, Category category, Integer stock, Integer bareMinimun, Double net,
            Boolean ignoreStock) {
        this.id = id;
        this.name = name;
        this.price = price;
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

    public double getPrice() {
        return this.price;
    }

    public void setPrice(double price) {
        this.price = price;
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
                ", price='" + getPrice() + "'" +
                ", stock='" + getStock() + "'" +
                "}";
    }

}
