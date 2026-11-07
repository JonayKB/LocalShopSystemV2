package es.jonay.kb.shopsystem.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

public record StockMovementDto(Long id, Long itemId, String itemName, String type, String typeLabel,
        int quantity, int stockAfter, BigDecimal unitCost, Long tradeId, String note, String createdBy,
        Instant createdAt) {
}
