package at.htlle.dto;

public record RestaurantSummary(
        Long id,
        String name,
        String code,
        String defaultCurrency) {
}
