package es.jonay.kb.shopsystem.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Contador por serie para numerar de forma correlativa y sin huecos.
 * Se incrementa dentro de la misma transacción que guarda la venta, con
 * bloqueo de escritura: si la venta falla, el número no se consume.
 */
@Entity
@Table(name = "invoice_counters")
public class InvoiceCounter {

    @Id
    @Column(length = 20)
    private String series;

    @Column(nullable = false)
    private Long lastNumber = 0L;

    public InvoiceCounter() {
    }

    public InvoiceCounter(String series) {
        this.series = series;
    }

    public String getSeries() {
        return series;
    }

    public Long getLastNumber() {
        return lastNumber;
    }

    public long next() {
        lastNumber = lastNumber + 1;
        return lastNumber;
    }
}
