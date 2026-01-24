package at.htlle.dto;

public record RewardSummaryResponse(
        Long id,
        String name,
        String description,
        int costPoints) {
}
