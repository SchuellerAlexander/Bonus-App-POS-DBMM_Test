package at.htlle.dto;

import java.time.Instant;

public record RedemptionCodeResponse(
        String redemptionCode,
        boolean redeemed,
        Instant redeemedAt
) {
}
