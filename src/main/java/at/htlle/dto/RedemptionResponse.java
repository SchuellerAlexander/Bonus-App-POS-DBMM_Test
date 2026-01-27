package at.htlle.dto;

import java.time.Instant;

public record RedemptionResponse(
        Long redemptionId,
        Long accountId,
        Long rewardId,
        Long restaurantId,
        String redemptionCode,
        boolean redeemed,
        Long pointsSpent,
        Long balanceAfter,
        Instant createdAt,
        Instant redeemedAt) {
}
