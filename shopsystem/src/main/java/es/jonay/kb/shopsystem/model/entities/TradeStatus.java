package es.jonay.kb.shopsystem.model.entities;

/**
 * Estado de una venta/factura simplificada. Una venta emitida nunca se borra:
 * si hay que deshacerla pasa a CANCELLED (anulación) y conserva su registro.
 */
public enum TradeStatus {
    ISSUED,
    CANCELLED
}
