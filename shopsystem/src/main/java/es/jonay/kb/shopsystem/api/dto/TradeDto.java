package es.jonay.kb.shopsystem.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

public class TradeDto {
    private Long id;
    private List<ItemDto> items;
    private Date date;

    private String series;
    private Long number;
    /** Número legible, p. ej. "T-000123". Null en ventas anteriores a Verifactu. */
    private String invoiceNumber;
    /** ISSUED o CANCELLED. */
    private String status;
    private Date cancelledAt;
    private String cancellationReason;
    private List<TradeLineDto> lines = new ArrayList<>();
    private BigDecimal totalBase;
    private BigDecimal totalTax;
    private BigDecimal total;

    public TradeDto() {
    }

    public TradeDto(Long id, List<ItemDto> items, Date date) {
        this.id = id;
        this.items = items;
        this.date = date;
    }

    public TradeDto(List<ItemDto> items, Date date) {
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

    public String getSeries() {
        return series;
    }

    public void setSeries(String series) {
        this.series = series;
    }

    public Long getNumber() {
        return number;
    }

    public void setNumber(Long number) {
        this.number = number;
    }

    public String getInvoiceNumber() {
        return invoiceNumber;
    }

    public void setInvoiceNumber(String invoiceNumber) {
        this.invoiceNumber = invoiceNumber;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Date getCancelledAt() {
        return cancelledAt;
    }

    public void setCancelledAt(Date cancelledAt) {
        this.cancelledAt = cancelledAt;
    }

    public String getCancellationReason() {
        return cancellationReason;
    }

    public void setCancellationReason(String cancellationReason) {
        this.cancellationReason = cancellationReason;
    }

    public List<TradeLineDto> getLines() {
        return lines;
    }

    public void setLines(List<TradeLineDto> lines) {
        this.lines = lines;
    }

    public BigDecimal getTotalBase() {
        return totalBase;
    }

    public void setTotalBase(BigDecimal totalBase) {
        this.totalBase = totalBase;
    }

    public BigDecimal getTotalTax() {
        return totalTax;
    }

    public void setTotalTax(BigDecimal totalTax) {
        this.totalTax = totalTax;
    }

    public BigDecimal getTotal() {
        return total;
    }

    public void setTotal(BigDecimal total) {
        this.total = total;
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
