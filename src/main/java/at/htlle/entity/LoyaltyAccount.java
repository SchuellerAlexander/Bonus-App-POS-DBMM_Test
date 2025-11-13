package at.htlle.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "loyalty_account", uniqueConstraints = {
        @UniqueConstraint(name = "uk_loyalty_account_number", columnNames = "account_number"),
        @UniqueConstraint(name = "uk_loyalty_account_customer_restaurant", columnNames = {"customer_id", "restaurant_id"})
}, indexes = {
        @Index(name = "idx_loyalty_account_customer", columnList = "customer_id"),
        @Index(name = "idx_loyalty_account_restaurant", columnList = "restaurant_id")
})
public class LoyaltyAccount {

    public enum Status {
        ACTIVE,
        BLOCKED,
        CLOSED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Size(max = 40)
    @Column(name = "account_number", nullable = false, length = 40)
    private String accountNumber;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "restaurant_id", nullable = false)
    private Restaurant restaurant;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private Status status = Status.ACTIVE;

    @NotNull
    @PositiveOrZero
    @Column(name = "current_points", nullable = false)
    private Long currentPoints = 0L;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    @OneToMany(mappedBy = "loyaltyAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Purchase> purchases = new ArrayList<>();

    @OneToMany(mappedBy = "loyaltyAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<PointLedger> ledgerEntries = new ArrayList<>();

    @OneToMany(mappedBy = "loyaltyAccount", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private List<Redemption> redemptions = new ArrayList<>();

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Long getId() {
        return id;
    }

    public String getAccountNumber() {
        return accountNumber;
    }

    public void setAccountNumber(String accountNumber) {
        this.accountNumber = accountNumber;
    }

    public Customer getCustomer() {
        return customer;
    }

    public void setCustomer(Customer customer) {
        this.customer = customer;
    }

    public Restaurant getRestaurant() {
        return restaurant;
    }

    public void setRestaurant(Restaurant restaurant) {
        this.restaurant = restaurant;
    }

    public Status getStatus() {
        return status;
    }

    public void setStatus(Status status) {
        this.status = status;
    }

    public Long getCurrentPoints() {
        return currentPoints;
    }

    public void setCurrentPoints(Long currentPoints) {
        this.currentPoints = currentPoints;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public List<Purchase> getPurchases() {
        return purchases;
    }

    public void setPurchases(List<Purchase> purchases) {
        this.purchases = purchases;
    }

    public List<PointLedger> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<PointLedger> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }

    public List<Redemption> getRedemptions() {
        return redemptions;
    }

    public void setRedemptions(List<Redemption> redemptions) {
        this.redemptions = redemptions;
    }
}
