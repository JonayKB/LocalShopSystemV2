package es.jonay.kb.shopsystem.model.entities;

/**
 * Forma de pago de una venta. Las ventas anteriores a este campo quedan como
 * efectivo (valor por defecto de la columna).
 */
public enum PaymentMethod {
    CASH,
    CARD;

    /** Convierte el valor recibido del cliente; null o vacío = efectivo. */
    public static PaymentMethod parse(String value) {
        if (value == null || value.isBlank()) {
            return CASH;
        }
        try {
            return PaymentMethod.valueOf(value.trim().toUpperCase(java.util.Locale.ROOT));
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Forma de pago no válida: " + value + " (CASH o CARD)");
        }
    }

    /** Texto para tickets e informes. */
    public String label() {
        return this == CARD ? "Tarjeta" : "Efectivo";
    }
}
