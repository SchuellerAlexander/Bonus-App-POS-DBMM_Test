package at.htlle.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record RedemptionRequest(
        @NotNull Long accountId,
        @NotNull Long rewardId,
        @NotNull Long restaurantId,
        @Size(max = 255) String notes) {
}
