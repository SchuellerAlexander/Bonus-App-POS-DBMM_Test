package at.htlle.dto;

import at.htlle.entity.Redemption;
import java.time.Instant;

public record RedemptionResponse(
        Long redemptionId,
        String redemptionCode,
        Long accountId,
        Long rewardId,
        Long restaurantId,
        Long ledgerEntryId,
        Long pointsSpent,
        Long balanceAfter,
        Redemption.Status status,
        Instant redeemedAt) {
}
