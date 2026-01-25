package at.htlle.dto;

import java.math.BigDecimal;

public record AdminPointRuleRequest(
        BigDecimal pointsPerEuro,
        boolean active
) {
}
