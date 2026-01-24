package at.htlle.dto;

public record RestaurantSummaryResponse(
        Long id,
        String name,
        Long defaultBranchId) {
}
