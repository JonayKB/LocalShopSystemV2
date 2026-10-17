package es.jonay.kb.shopsystem.model.entities;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Objects;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.ManyToMany;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(name = "Trades", uniqueConstraints = @UniqueConstraint(name = "uk_trade_series_number", columnNames = {
        "series", "number" }))
public class Trade {
    @Id
    @GeneratedValue(strategy = GenerationType.AUTO)
    private Long id;

    /**
     * Se mantiene por compatibilidad con ventas antiguas y con las consultas de
     * gráficas. Las ventas nuevas también rellenan {@link #lines}.
     */
    @ManyToMany()
    private List<Item> items;

    /** Líneas inmutables de la venta (precio, IGIC y cantidad al vender). */
    @OneToMany(mappedBy = "trade", cascade = CascadeType.ALL)
    private List<TradeLine> lines = new ArrayList<>();

    /** Instante real de emisión. */
    private Date date;

    /** Serie de la factura simplificada (p. ej. "T"). Null en ventas anteriores a Verifactu. */
    @Column(length = 20)
    private String series;

    /** Número correlativo dentro de la serie, sin huecos. Null en ventas anteriores. */
    private Long number;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20, columnDefinition = "varchar(20) not null default 'ISSUED'")
    private TradeStatus status = TradeStatus.ISSUED;

    private Date cancelledAt;

    @Column(length = 255)
    private String cancellationReason;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalBase;

    @Column(precision = 12, scale = 2)
    private BigDecimal totalTax;

    @Column(precision = 12, scale = 2)
    private BigDecimal total;

    public Trade() {
    }

    public Trade(Long id, List<Item> items) {
        this.id = id;
        this.items = items;
    }

    /** Una venta emitida no se borra: se anula. */
    @PreRemove
    private void forbidRemove() {
        throw new IllegalStateException("Las ventas no se pueden borrar; hay que anularlas");
    }

    public void addLine(TradeLine line) {
        line.setTrade(this);
        this.lines.add(line);
    }

    public Long getId() {
        return this.id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public List<Item> getItems() {
        return this.items;
    }

    public void setItems(List<Item> items) {
        this.items = items;
    }

    public List<TradeLine> getLines() {
        return lines;
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

    public TradeStatus getStatus() {
        return status;
    }

    public void setStatus(TradeStatus status) {
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
        if (!(o instanceof Trade)) {
            return false;
        }
        Trade trade = (Trade) o;
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
                ", series='" + getSeries() + "'" +
                ", number='" + getNumber() + "'" +
                ", status='" + getStatus() + "'" +
                ", date='" + getDate() + "'" +
                "}";
    }

}
