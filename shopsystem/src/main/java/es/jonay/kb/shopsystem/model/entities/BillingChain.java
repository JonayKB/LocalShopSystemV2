package es.jonay.kb.shopsystem.model.entities;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * Cabeza de la cadena de registros de facturación (una sola fila, id "MAIN").
 * Se bloquea con PESSIMISTIC_WRITE mientras se genera cada registro, de modo que
 * dos ventas simultáneas nunca encadenan con el mismo registro anterior.
 */
@Entity
@Table(name = "billing_chain")
public class BillingChain {

    public static final String MAIN = "MAIN";

    @Id
    @Column(length = 10)
    private String id = MAIN;

    /** Huella del último registro generado. Null si aún no hay ninguno. */
    @Column(length = 64)
    private String lastHash;

    @Column(length = 9)
    private String lastNif;

    @Column(length = 60)
    private String lastNumSerie;

    @Column(length = 10)
    private String lastFecha;

    public BillingChain() {
    }

    public String getId() { return id; }
    public String getLastHash() { return lastHash; }
    public String getLastNif() { return lastNif; }
    public String getLastNumSerie() { return lastNumSerie; }
    public String getLastFecha() { return lastFecha; }

    public void advance(String hash, String nif, String numSerie, String fecha) {
        this.lastHash = hash;
        this.lastNif = nif;
        this.lastNumSerie = numSerie;
        this.lastFecha = fecha;
    }
}
