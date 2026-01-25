package at.htlle.service;

import at.htlle.entity.Customer;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.PointLedger;
import at.htlle.repository.CustomerRepository;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.PointLedgerRepository;
import jakarta.transaction.Transactional;
import java.time.Instant;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AdminManagementService {

    private static final String FIXED_ADMIN_USERNAME = "admin";

    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointLedgerRepository pointLedgerRepository;

    public AdminManagementService(CustomerRepository customerRepository,
                                  LoyaltyAccountRepository loyaltyAccountRepository,
                                  PointLedgerRepository pointLedgerRepository) {
        this.customerRepository = customerRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.pointLedgerRepository = pointLedgerRepository;
    }

    @Transactional
    public Optional<String> updateCustomerRole(Long customerId, Customer.Role role) {
        Customer customer = customerRepository.findById(customerId)
                .orElse(null);
        if (customer == null) {
            return Optional.of("Customer not found.");
        }
        if (isFixedAdmin(customer)) {
            return Optional.of("Admin user cannot be modified.");
        }
        customer.setRole(role);
        customerRepository.save(customer);
        return Optional.empty();
    }

    @Transactional
    public Optional<String> updateCustomerStatus(Long customerId, Customer.Status status) {
        Customer customer = customerRepository.findById(customerId)
                .orElse(null);
        if (customer == null) {
            return Optional.of("Customer not found.");
        }
        if (isFixedAdmin(customer)) {
            return Optional.of("Admin user cannot be modified.");
        }
        customer.setStatus(status);
        customerRepository.save(customer);
        return Optional.empty();
    }

    @Transactional
    public Optional<String> deleteCustomer(Long customerId) {
        Customer customer = customerRepository.findById(customerId)
                .orElse(null);
        if (customer == null) {
            return Optional.of("Customer not found.");
        }
        if (isFixedAdmin(customer)) {
            return Optional.of("Admin user cannot be deleted.");
        }
        customerRepository.delete(customer);
        return Optional.empty();
    }

    @Transactional
    public Optional<String> adjustPoints(Long accountId, Long pointsDelta, String reason) {
        if (pointsDelta == null || pointsDelta == 0) {
            return Optional.of("Points delta must not be zero.");
        }
        if (!StringUtils.hasText(reason)) {
            return Optional.of("Reason is required.");
        }
        LoyaltyAccount account = loyaltyAccountRepository.lockById(accountId)
                .orElse(null);
        if (account == null) {
            return Optional.of("Loyalty account not found.");
        }
        long newBalance = account.getCurrentPoints() + pointsDelta;
        if (newBalance < 0) {
            return Optional.of("Resulting balance must not be negative.");
        }
        account.setCurrentPoints(newBalance);
        loyaltyAccountRepository.save(account);

        PointLedger entry = new PointLedger();
        entry.setLoyaltyAccount(account);
        entry.setEntryType(PointLedger.EntryType.ADJUST);
        entry.setPoints(pointsDelta);
        entry.setBalanceAfter(newBalance);
        entry.setOccurredAt(Instant.now());
        entry.setDescription(reason.trim());
        pointLedgerRepository.save(entry);
        return Optional.empty();
    }

    private boolean isFixedAdmin(Customer customer) {
        return customer != null
                && customer.getUsername() != null
                && customer.getUsername().equalsIgnoreCase(FIXED_ADMIN_USERNAME);
    }
}
