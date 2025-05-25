package es.jonay.kb.shopsystem.model.entities;

import java.util.Set;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.Objects;

@Entity
@Table(name = "Categories")
public class Category {

    @Id
    private Long id;
    private String name;
    @OneToMany(mappedBy = "category")
    private Set<Item> items;

    private String image;

    public Category() {
    }

    public Category(Long id, String name, Set<Item> items) {
        this.id = id;
        this.name = name;
        this.items = items;
    }

    public Category(Long id) {
        this.id = id;
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

    public Set<Item> getItems() {
        return this.items;
    }

    public void setItems(Set<Item> items) {
        this.items = items;
    }

    public String getImage() {
        return this.image;
    }

    public void setImage(String image) {
        this.image = image;
    }

    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof Category)) {
            return false;
        }
        Category category = (Category) o;
        return Objects.equals(id, category.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "{" +
                " categoryId='" + getId() + "'" +
                ", name='" + getName() + "'" +
                ", items='" + getItems() + "'" +
                "}";
    }

}
