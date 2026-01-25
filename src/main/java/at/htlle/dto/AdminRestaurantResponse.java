package at.htlle.dto;

public record AdminRestaurantResponse(
        Long id,
        String name,
        String code,
        String defaultCurrency,
        boolean active
) {
}
