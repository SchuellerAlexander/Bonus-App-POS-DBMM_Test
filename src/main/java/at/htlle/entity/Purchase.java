package at.htlle.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PastOrPresent;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "purchase", uniqueConstraints = {
        @UniqueConstraint(name = "uk_purchase_number", columnNames = "purchase_number")
}, indexes = {
        @Index(name = "idx_purchase_account", columnList = "loyalty_account_id"),
        @Index(name = "idx_purchase_branch", columnList = "branch_id"),
        @Index(name = "idx_purchase_occurred_at", columnList = "purchased_at")
})
public class Purchase {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "loyalty_account_id", nullable = false)
    private LoyaltyAccount loyaltyAccount;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "branch_id")
    private Branch branch;

    @NotBlank
    @Size(max = 40)
    @Column(name = "purchase_number", nullable = false, length = 40)
    private String purchaseNumber;

    @NotNull
    @PositiveOrZero
    @Digits(integer = 12, fraction = 2)
    @Column(name = "total_amount", nullable = false, precision = 12, scale = 2)
    private BigDecimal totalAmount;

    @NotBlank
    @Size(min = 3, max = 3)
    @Column(name = "currency", nullable = false, length = 3)
    private String currency;

    @PastOrPresent
    @Column(name = "purchased_at", nullable = false)
    private Instant purchasedAt;

    @Size(max = 255)
    @Column(name = "notes", length = 255)
    private String notes;

    @OneToMany(mappedBy = "purchase")
    private List<PointLedger> ledgerEntries = new ArrayList<>();

    @PrePersist
    void onCreate() {
        if (purchasedAt == null) {
            purchasedAt = Instant.now();
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

    public Branch getBranch() {
        return branch;
    }

    public void setBranch(Branch branch) {
        this.branch = branch;
    }

    public String getPurchaseNumber() {
        return purchaseNumber;
    }

    public void setPurchaseNumber(String purchaseNumber) {
        this.purchaseNumber = purchaseNumber;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCurrency() {
        return currency;
    }

    public void setCurrency(String currency) {
        this.currency = currency;
    }

    public Instant getPurchasedAt() {
        return purchasedAt;
    }

    public void setPurchasedAt(Instant purchasedAt) {
        this.purchasedAt = purchasedAt;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public List<PointLedger> getLedgerEntries() {
        return ledgerEntries;
    }

    public void setLedgerEntries(List<PointLedger> ledgerEntries) {
        this.ledgerEntries = ledgerEntries;
    }
}
