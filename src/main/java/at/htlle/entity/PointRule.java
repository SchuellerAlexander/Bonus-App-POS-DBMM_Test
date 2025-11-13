package at.htlle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "point_rule", uniqueConstraints = {
        @UniqueConstraint(name = "uk_point_rule_name_per_restaurant", columnNames = {"restaurant_id", "name"})
}, indexes = {
        @Index(name = "idx_point_rule_restaurant", columnList = "restaurant_id")
})
public class PointRule {

    public enum RuleType {
        MULTIPLIER,
        FIXED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @NotBlank
    @Size(max = 120)
    @Column(name = "name", nullable = false, length = 120)
    private String name;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(name = "rule_type", nullable = false, length = 20)
    private RuleType ruleType = RuleType.MULTIPLIER;

    @NotNull
    @DecimalMin(value = "0.0")
    @Digits(integer = 5, fraction = 2)
    @Column(name = "multiplier", nullable = false, precision = 7, scale = 2)
    private BigDecimal multiplier = BigDecimal.ONE;

    @NotNull
    @DecimalMin(value = "0.0")
    @Digits(integer = 8, fraction = 2)
    @Column(name = "amount_threshold", nullable = false, precision = 10, scale = 2)
    private BigDecimal amountThreshold = BigDecimal.ZERO;

    @NotNull
    @Column(name = "base_points", nullable = false)
    private Integer basePoints = 0;

    @Column(name = "valid_from")
    private LocalDate validFrom;

    @Column(name = "valid_until")
    private LocalDate validUntil;

    @Column(name = "active", nullable = false)
    private boolean active = true;

    public Long getId() {
        return id;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public RuleType getRuleType() {
        return ruleType;
    }

    public void setRuleType(RuleType ruleType) {
        this.ruleType = ruleType;
    }

    public BigDecimal getMultiplier() {
        return multiplier;
    }

    public void setMultiplier(BigDecimal multiplier) {
        this.multiplier = multiplier;
    }

    public BigDecimal getAmountThreshold() {
        return amountThreshold;
    }

    public void setAmountThreshold(BigDecimal amountThreshold) {
        this.amountThreshold = amountThreshold;
    }

    public Integer getBasePoints() {
        return basePoints;
    }

    public void setBasePoints(Integer basePoints) {
        this.basePoints = basePoints;
    }

    public LocalDate getValidFrom() {
        return validFrom;
    }

    public void setValidFrom(LocalDate validFrom) {
        this.validFrom = validFrom;
    }

    public LocalDate getValidUntil() {
        return validUntil;
    }

    public void setValidUntil(LocalDate validUntil) {
        this.validUntil = validUntil;
    }

    public boolean isActive() {
        return active;
    }

    public void setActive(boolean active) {
        this.active = active;
    }
}
