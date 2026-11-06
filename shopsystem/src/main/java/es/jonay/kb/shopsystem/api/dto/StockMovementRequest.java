package es.jonay.kb.shopsystem.api.dto;

import java.math.BigDecimal;
import java.util.List;

/**
 * Registro manual de movimientos de stock (entrada, devolución, merma o ajuste).
 * En RESTOCK, SUPPLIER_RETURN y LOSS quantity es positiva (el signo lo pone el tipo).
 * En ADJUSTMENT se usa countedStock (recuento real) o quantity con signo.
 * unitCost opcional: en una entrada, si se indica y es distinto, actualiza el coste del producto.
 */
public record StockMovementRequest(String type, String note, List<Line> lines) {
    public record Line(Long itemId, Integer quantity, Integer countedStock, BigDecimal unitCost) {
    }
}
