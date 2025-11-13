package at.htlle.dto;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;

public record PurchaseRequest(
        @NotNull Long accountId,
        Long branchId,
        @NotBlank @Size(max = 40) String purchaseNumber,
        @NotNull @PositiveOrZero @Digits(integer = 12, fraction = 2) BigDecimal totalAmount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        Instant purchasedAt,
        @Size(max = 255) String notes,
        @Size(max = 255) String description,
        Long pointRuleId) {
}
