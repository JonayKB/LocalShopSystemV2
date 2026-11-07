package es.jonay.kb.shopsystem.model.entities;

/**
 * Tipos de movimiento de stock. {@link #sign()} indica si suma (+1), resta (-1) o puede ir
 * en ambos sentidos (0, ajustes de inventario).
 */
public enum StockMovementType {
    /** Saldo inicial al activar el registro de movimientos. */
    OPENING_BALANCE(1, "Saldo inicial", false),
    /** Venta (la genera el sistema). */
    SALE(-1, "Venta", false),
    /** Anulación de una venta: vuelve el stock (la genera el sistema). */
    SALE_CANCEL(1, "Venta anulada", false),
    /** Entrada de mercancía del proveedor. */
    RESTOCK(1, "Entrada de mercancía", true),
    /** Devolución al proveedor. */
    SUPPLIER_RETURN(-1, "Devolución al proveedor", true),
    /** Merma: rotura, caducidad, robo, consumo propio... */
    LOSS(-1, "Merma", true),
    /** Ajuste por recuento de inventario o corrección manual (+ o -). */
    ADJUSTMENT(0, "Ajuste de inventario", true);

    private final int sign;
    private final String label;
    private final boolean manual;

    StockMovementType(int sign, String label, boolean manual) {
        this.sign = sign;
        this.label = label;
        this.manual = manual;
    }

    public int sign() {
        return sign;
    }

    public String label() {
        return label;
    }

    /** Si se puede registrar a mano desde la pantalla de stock. */
    public boolean isManual() {
        return manual;
    }
}
