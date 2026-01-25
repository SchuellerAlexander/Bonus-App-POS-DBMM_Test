package at.htlle.controller;

import at.htlle.dto.AccountResponse;
import at.htlle.dto.LedgerEntryResponse;
import at.htlle.dto.PurchaseRequest;
import at.htlle.dto.PurchaseResponse;
import at.htlle.dto.PurchaseDetailsResponse;
import at.htlle.dto.RedemptionRequest;
import at.htlle.dto.RedemptionResponse;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.PointLedger;
import at.htlle.entity.Purchase;
import at.htlle.entity.Redemption;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.PointLedgerRepository;
import at.htlle.service.LoyaltyService;
import jakarta.persistence.EntityNotFoundException;
import jakarta.validation.Valid;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class LoyaltyController {

    private final LoyaltyService loyaltyService;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointLedgerRepository pointLedgerRepository;

    public LoyaltyController(
            LoyaltyService loyaltyService,
            LoyaltyAccountRepository loyaltyAccountRepository,
            PointLedgerRepository pointLedgerRepository) {
        this.loyaltyService = loyaltyService;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.pointLedgerRepository = pointLedgerRepository;
    }

    @PostMapping("/purchases")
    public ResponseEntity<PurchaseResponse> recordPurchase(@Valid @RequestBody PurchaseRequest request) {
        PointLedger ledger = loyaltyService.recordPurchase(request);
        Purchase purchase = Objects.requireNonNull(ledger.getPurchase(), "purchase");

        PurchaseResponse response = new PurchaseResponse(
                purchase.getId(),
                purchase.getPurchaseNumber(),
                purchase.getTotalAmount(),
                purchase.getCurrency(),
                purchase.getPurchasedAt(),
                ledger.getLoyaltyAccount().getId(),
                purchase.getRestaurant().getId(),
                ledger.getId(),
                ledger.getPoints(),
                ledger.getBalanceAfter());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/redemptions")
    public ResponseEntity<RedemptionResponse> redeemReward(@Valid @RequestBody RedemptionRequest request) {
        Redemption redemption = loyaltyService.redeemReward(request);
        PointLedger ledger = redemption.getLedgerEntry();

        RedemptionResponse response = new RedemptionResponse(
                redemption.getId(),
                redemption.getLoyaltyAccount().getId(),
                redemption.getReward().getId(),
                redemption.getRestaurant().getId(),
                ledger.getId(),
                redemption.getPointsSpent(),
                ledger.getBalanceAfter(),
                redemption.getStatus(),
                redemption.getRedeemedAt());

        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @PostMapping("/accounts/{id}/sync")
    public AccountResponse synchronizeBalance(@PathVariable("id") Long accountId,
                                              @RequestParam(defaultValue = "false") boolean includeLedger) {
        LoyaltyAccount account = loyaltyService.synchronizeBalance(accountId);
        return buildAccountResponse(account, includeLedger);
    }

    @GetMapping("/accounts/{id}")
    public AccountResponse getAccount(@PathVariable("id") Long accountId,
                                      @RequestParam(defaultValue = "false") boolean includeLedger) {
        LoyaltyAccount account = loyaltyAccountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found"));
        return buildAccountResponse(account, includeLedger);
    }

    @GetMapping("/ledger/{id}/purchase")
    public PurchaseDetailsResponse getPurchaseDetails(@PathVariable("id") Long ledgerId) {
        PointLedger ledger = pointLedgerRepository.findById(ledgerId)
                .orElseThrow(() -> new EntityNotFoundException("Ledger entry not found"));
        Purchase purchase = ledger.getPurchase();
        if (purchase == null) {
            throw new EntityNotFoundException("Purchase not found for ledger entry");
        }

        return new PurchaseDetailsResponse(
                ledger.getLoyaltyAccount().getId(),
                purchase.getRestaurant().getId(),
                purchase.getPurchaseNumber(),
                purchase.getTotalAmount(),
                purchase.getCurrency(),
                purchase.getNotes(),
                ledger.getDescription());
    }

    private AccountResponse buildAccountResponse(LoyaltyAccount account, boolean includeLedger) {
        List<LedgerEntryResponse> ledgerEntries = Collections.emptyList();
        if (includeLedger) {
            ledgerEntries = pointLedgerRepository.findByLoyaltyAccountIdOrderByOccurredAtDesc(account.getId())
                    .stream()
                    .map(this::toLedgerEntryResponse)
                    .collect(Collectors.toList());
        }

        return new AccountResponse(
                account.getId(),
                account.getAccountNumber(),
                account.getCustomer().getId(),
                account.getCustomer().getFirstName(),
                account.getCustomer().getLastName(),
                account.getRestaurant().getId(),
                account.getStatus(),
                account.getTier(),
                account.getCurrentPoints(),
                account.getCreatedAt(),
                account.getUpdatedAt(),
                ledgerEntries);
    }

    private LedgerEntryResponse toLedgerEntryResponse(PointLedger entry) {
        return new LedgerEntryResponse(
                entry.getId(),
                entry.getEntryType(),
                entry.getPoints(),
                entry.getBalanceAfter(),
                entry.getOccurredAt(),
                entry.getDescription(),
                entry.getPurchase() != null ? entry.getPurchase().getId() : null,
                entry.getPointRule() != null ? entry.getPointRule().getId() : null,
                entry.getRedemption() != null ? entry.getRedemption().getId() : null);
    }
}
