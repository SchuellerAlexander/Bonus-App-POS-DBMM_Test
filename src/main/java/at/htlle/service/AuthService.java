package at.htlle.service;

import at.htlle.entity.Customer;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.Restaurant;
import at.htlle.repository.CustomerRepository;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.RestaurantRepository;
import jakarta.persistence.EntityNotFoundException;
import java.util.List;
import java.util.Optional;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class AuthService {

    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final RestaurantRepository restaurantRepository;
    private final PasswordEncoder passwordEncoder;

    public AuthService(CustomerRepository customerRepository,
                       LoyaltyAccountRepository loyaltyAccountRepository,
                       RestaurantRepository restaurantRepository,
                       PasswordEncoder passwordEncoder) {
        this.customerRepository = customerRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.restaurantRepository = restaurantRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public LoyaltyAccount register(String firstName,
                                   String lastName,
                                   String email,
                                   String username,
                                   String password) {
        String normalizedUsername = username.trim();
        String normalizedEmail = email.trim().toLowerCase();
        if (customerRepository.findByUsername(normalizedUsername).isPresent()) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (customerRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new IllegalArgumentException("Email already exists");
        }

        Customer customer = new Customer();
        customer.setFirstName(firstName.trim());
        customer.setLastName(lastName.trim());
        customer.setEmail(normalizedEmail);
        customer.setUsername(normalizedUsername);
        customer.setPassword(passwordEncoder.encode(password));
        customer.setRole(Customer.Role.USER);
        Customer savedCustomer = customerRepository.save(customer);

        Restaurant restaurant = restaurantRepository.findByCode("DEMO")
                .orElseThrow(() -> new EntityNotFoundException("Restaurant not found"));

        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomer(savedCustomer);
        account.setRestaurant(restaurant);
        account.setAccountNumber(buildAccountNumber(savedCustomer.getId()));

        return loyaltyAccountRepository.save(account);
    }

    public Optional<Long> resolveAccountId(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        Customer customer = customerRepository.findByUsername(username.trim())
                .orElseThrow(() -> new EntityNotFoundException("Customer not found for username"));
        List<LoyaltyAccount> accounts = loyaltyAccountRepository.findByCustomerIdOrderByIdAsc(customer.getId());
        if (accounts.isEmpty()) {
            throw new IllegalStateException("No loyalty account found for customer " + customer.getId());
        }
        if (accounts.size() > 1) {
            throw new IllegalStateException("Multiple loyalty accounts found for customer " + customer.getId());
        }
        return Optional.of(accounts.get(0).getId());
    }

    private String buildAccountNumber(Long customerId) {
        return String.format("ACCT-%04d", customerId);
    }
}
