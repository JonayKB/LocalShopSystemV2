package es.jonay.kb.shopsystem.model.entities;

public enum BillingSendStatus {
    /** Generado, pendiente de enviar (o de reintentar). */
    PENDING,
    /** La AEAT lo aceptó sin errores. */
    ACCEPTED,
    /** La AEAT lo aceptó pero con errores (p. ej. huella que no coincide). Hay que revisarlo. */
    ACCEPTED_WITH_ERRORS,
    /** La AEAT lo rechazó. Hay que corregir y enviar una subsanación (revisión manual). */
    REJECTED
}
