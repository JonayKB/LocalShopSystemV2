package es.jonay.kb.shopsystem.api.dto;

import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TradeDto {
    private Long id;
    private List<ItemDto> items;
    private Date date;

    public TradeDto() {
    }

    public TradeDto(Long id, List<ItemDto> items, Date date) {
        this.id = id;
        this.items = items;
        this.date = date;
    }
    public TradeDto(List<ItemDto> items,Date date) {
        this.items = items;
        this.date = date;
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<ItemDto> getItems() {
        return this.items;
    }

    public void setItems(List<ItemDto> items) {
        this.items = items;
    }

    public Date getDate() {
        return this.date;
    }

    public void setDate(Date date) {
        this.date = date;
    }


    @Override
    public boolean equals(Object o) {
        if (o == this)
            return true;
        if (!(o instanceof TradeDto)) {
            return false;
        }
        TradeDto trade = (TradeDto) o;
        return Objects.equals(id, trade.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return "{" +
            " id='" + getId() + "'" +
            ", items='" + getItems() + "'" +
            ", items='" + getDate() + "'" +
            "}";
    }
    
}
