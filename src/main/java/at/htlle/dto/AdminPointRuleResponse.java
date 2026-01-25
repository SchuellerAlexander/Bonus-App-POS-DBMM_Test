package at.htlle.dto;

import java.math.BigDecimal;

public record AdminPointRuleResponse(
        Long id,
        Long restaurantId,
        String name,
        BigDecimal multiplier,
        boolean active
) {
}
