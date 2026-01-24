package at.htlle.dto;

import java.math.BigDecimal;

public record PurchaseDetailsResponse(
        Long accountId,
        Long restaurantId,
        String purchaseNumber,
        BigDecimal totalAmount,
        String currency,
        String notes,
        String description) {
}
