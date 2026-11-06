package es.jonay.kb.shopsystem.api.dto.analytics;

import java.math.BigDecimal;
import java.util.Date;

import es.jonay.kb.shopsystem.model.entities.PaymentMethod;

/** Una línea de venta emitida, tal y como se congeló al vender (proyección JPQL). */
public record SaleLineRow(Long tradeId, Date date, PaymentMethod paymentMethod, Long itemId, String description,
        String categoryName, Integer quantity, BigDecimal lineTotal, BigDecimal taxBase, BigDecimal taxAmount,
        BigDecimal unitCost) {
}
