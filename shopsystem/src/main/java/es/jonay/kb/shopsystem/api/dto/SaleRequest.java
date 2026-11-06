package es.jonay.kb.shopsystem.api.dto;

import java.util.List;

/**
 * Venta desde la caja: una línea por producto con su cantidad (no un elemento por unidad).
 * paymentMethod: CASH o CARD. print: imprimir el ticket.
 */
public record SaleRequest(List<Line> lines, String paymentMethod, boolean print) {
    public record Line(Long itemId, Integer quantity) {
    }
}
