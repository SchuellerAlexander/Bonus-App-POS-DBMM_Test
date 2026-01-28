package at.htlle.dto;

import at.htlle.entity.PointLedger;
import java.time.Instant;

public record LedgerEntryResponse(
        Long id,
        PointLedger.EntryType entryType,
        Long points,
        Long balanceAfter,
        Instant occurredAt,
        String description,
        Long purchaseId,
        Long pointRuleId,
        Long redemptionId,
        String redemptionCode) {
}
