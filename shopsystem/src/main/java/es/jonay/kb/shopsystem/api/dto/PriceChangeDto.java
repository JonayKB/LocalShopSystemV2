package es.jonay.kb.shopsystem.api.dto;

import java.math.BigDecimal;
import java.time.Instant;

/** Un cambio de precio. margin = % de margen sobre el precio sin IGIC (null si no hay coste). */
public record PriceChangeDto(Long id, BigDecimal price, BigDecimal cost, BigDecimal margin, Instant validFrom,
        String changedBy, String reason) {
}
