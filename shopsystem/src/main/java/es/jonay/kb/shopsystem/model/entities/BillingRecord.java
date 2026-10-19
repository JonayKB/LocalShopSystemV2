package es.jonay.kb.shopsystem.model.entities;

import java.math.BigDecimal;
import java.util.Date;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.PreRemove;
import jakarta.persistence.Table;

/**
 * Registro de facturación VERI*FACTU (alta o anulación). Solo inserción: los datos
 * del registro (columnas con updatable=false) no se modifican nunca; solo cambian
 * las columnas de estado de envío.
 */
@Entity
@Table(name = "billing_records", indexes = {
        @Index(name = "idx_billing_status", columnList = "status,id"),
        @Index(name = "idx_billing_trade", columnList = "tradeId") })
public class BillingRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, updatable = false, length = 12)
    private BillingRecordType type;

    @Column(nullable = false, updatable = false)
    private Long tradeId;

    /** NIF del obligado a expedir. */
    @Column(nullable = false, updatable = false, length = 9)
    private String nif;

    /** Serie + número, p. ej. "T-000123". */
    @Column(nullable = false, updatable = false, length = 60)
    private String numSerie;

    /** Fecha de expedición en formato DD-MM-AAAA (la misma que va en el XML y en el QR). */
    @Column(nullable = false, updatable = false, length = 10)
    private String fechaExpedicion;

    @Column(updatable = false, precision = 12, scale = 2)
    private BigDecimal importeTotal;

    /** FechaHoraHusoGenRegistro tal cual se usó en la huella, p. ej. 2026-10-18T11:00:00+01:00. */
    @Column(nullable = false, updatable = false, length = 32)
    private String generatedAt;

    /** Huella del registro anterior (null si es el primero de la cadena). */
    @Column(updatable = false, length = 64)
    private String previousHash;

    @Column(nullable = false, updatable = false, length = 64)
    private String hash;

    /** Fragmento XML del registro tal cual se envía a la AEAT. */
    @Column(nullable = false, updatable = false, length = 8000)
    private String registroXml;

    // ---- Estado de envío (lo único que cambia) ----

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private BillingSendStatus status = BillingSendStatus.PENDING;

    private int attempts;

    private Date lastAttemptAt;

    private Date nextAttemptAt;

    private Date sentAt;

    /** CSV de 16 caracteres devuelto por la AEAT al aceptar el envío. */
    @Column(length = 32)
    private String csv;

    @Column(length = 10)
    private String aeatErrorCode;

    @Column(length = 1500)
    private String aeatErrorDescription;

    public BillingRecord() {
    }

    /** Un registro de facturación no se borra jamás. */
    @PreRemove
    private void forbidRemove() {
        throw new IllegalStateException("Los registros de facturación no se pueden borrar");
    }

    public static BillingRecord of(BillingRecordType type, Long tradeId, String nif, String numSerie, String fecha,
            BigDecimal importeTotal, String generatedAt, String previousHash, String hash, String registroXml) {
        BillingRecord r = new BillingRecord();
        r.type = type;
        r.tradeId = tradeId;
        r.nif = nif;
        r.numSerie = numSerie;
        r.fechaExpedicion = fecha;
        r.importeTotal = importeTotal;
        r.generatedAt = generatedAt;
        r.previousHash = previousHash;
        r.hash = hash;
        r.registroXml = registroXml;
        return r;
    }

    public Long getId() { return id; }
    public BillingRecordType getType() { return type; }
    public Long getTradeId() { return tradeId; }
    public String getNif() { return nif; }
    public String getNumSerie() { return numSerie; }
    public String getFechaExpedicion() { return fechaExpedicion; }
    public BigDecimal getImporteTotal() { return importeTotal; }
    public String getGeneratedAt() { return generatedAt; }
    public String getPreviousHash() { return previousHash; }
    public String getHash() { return hash; }
    public String getRegistroXml() { return registroXml; }
    public BillingSendStatus getStatus() { return status; }
    public void setStatus(BillingSendStatus status) { this.status = status; }
    public int getAttempts() { return attempts; }
    public void setAttempts(int attempts) { this.attempts = attempts; }
    public Date getLastAttemptAt() { return lastAttemptAt; }
    public void setLastAttemptAt(Date lastAttemptAt) { this.lastAttemptAt = lastAttemptAt; }
    public Date getNextAttemptAt() { return nextAttemptAt; }
    public void setNextAttemptAt(Date nextAttemptAt) { this.nextAttemptAt = nextAttemptAt; }
    public Date getSentAt() { return sentAt; }
    public void setSentAt(Date sentAt) { this.sentAt = sentAt; }
    public String getCsv() { return csv; }
    public void setCsv(String csv) { this.csv = csv; }
    public String getAeatErrorCode() { return aeatErrorCode; }
    public void setAeatErrorCode(String aeatErrorCode) { this.aeatErrorCode = aeatErrorCode; }
    public String getAeatErrorDescription() { return aeatErrorDescription; }
    public void setAeatErrorDescription(String d) {
        this.aeatErrorDescription = d == null ? null : (d.length() > 1500 ? d.substring(0, 1500) : d);
    }
}
