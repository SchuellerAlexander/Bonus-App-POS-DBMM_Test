package at.htlle.dto;

public record AdminRestaurantRequest(
        String name,
        String code,
        String defaultCurrency,
        boolean active
) {
}
