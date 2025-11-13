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
import jakarta.persistence.OneToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.time.Instant;

@Entity
@Table(name = "point_ledger", indexes = {
        @Index(name = "idx_point_ledger_account", columnList = "loyalty_account_id"),
        @Index(name = "idx_point_ledger_purchase", columnList = "purchase_id"),
        @Index(name = "idx_point_ledger_rule", columnList = "point_rule_id")
})
public class PointLedger {

    public enum EntryType {
        EARN,
        REDEEM,
        ADJUST,
        EXPIRE
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loyalty_account_id", nullable = false)
    private LoyaltyAccount loyaltyAccount;

    @Enumerated(EnumType.STRING)
    @Column(name = "entry_type", nullable = false, length = 20)
    private EntryType entryType;

    @NotNull
    @Column(name = "points", nullable = false)
    private Long points;

    @PositiveOrZero
    @Column(name = "balance_after", nullable = false)
    private Long balanceAfter;

    @PastOrPresent
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;

    @Size(max = 255)
    @Column(name = "description", length = 255)
    private String description;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "purchase_id")
    private Purchase purchase;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "point_rule_id")
    private PointRule pointRule;

    @OneToOne(mappedBy = "ledgerEntry", fetch = FetchType.LAZY)
    private Redemption redemption;

    @PrePersist
    void onCreate() {
        if (occurredAt == null) {
            occurredAt = Instant.now();
        }
    }

    public Long getId() {
        return id;
    }

    public LoyaltyAccount getLoyaltyAccount() {
        return loyaltyAccount;
    }

    public void setLoyaltyAccount(LoyaltyAccount loyaltyAccount) {
        this.loyaltyAccount = loyaltyAccount;
    }

    public EntryType getEntryType() {
        return entryType;
    }

    public void setEntryType(EntryType entryType) {
        this.entryType = entryType;
    }

    public Long getPoints() {
        return points;
    }

    public void setPoints(Long points) {
        this.points = points;
    }

    public Long getBalanceAfter() {
        return balanceAfter;
    }

    public void setBalanceAfter(Long balanceAfter) {
        this.balanceAfter = balanceAfter;
    }

    public Instant getOccurredAt() {
        return occurredAt;
    }

    public void setOccurredAt(Instant occurredAt) {
        this.occurredAt = occurredAt;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Purchase getPurchase() {
        return purchase;
    }

    public void setPurchase(Purchase purchase) {
        this.purchase = purchase;
    }

    public PointRule getPointRule() {
        return pointRule;
    }

    public void setPointRule(PointRule pointRule) {
        this.pointRule = pointRule;
    }

    public Redemption getRedemption() {
        return redemption;
    }

    public void setRedemption(Redemption redemption) {
        this.redemption = redemption;
    }
}
