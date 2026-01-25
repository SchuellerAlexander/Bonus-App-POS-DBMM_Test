package at.htlle.service;

import at.htlle.entity.Customer;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.Restaurant;
import at.htlle.repository.CustomerRepository;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.RestaurantRepository;
import java.util.Comparator;
import java.util.Optional;
import org.springframework.transaction.annotation.Transactional;
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

        Restaurant restaurant = resolveOrCreateDefaultRestaurant();

        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomer(savedCustomer);
        account.setRestaurant(restaurant);
        account.setAccountNumber(buildAccountNumber(savedCustomer.getId()));

        return loyaltyAccountRepository.save(account);
    }

    @Transactional
    public Optional<Long> resolveAccountId(String username) {
        if (!StringUtils.hasText(username)) {
            return Optional.empty();
        }
        return customerRepository.findByUsername(username.trim())
                .flatMap(this::resolvePrimaryAccount)
                .map(LoyaltyAccount::getId);
    }

    private Optional<LoyaltyAccount> resolvePrimaryAccount(Customer customer) {
        Restaurant restaurant = resolveOrCreateDefaultRestaurant();
        return loyaltyAccountRepository.findByCustomerIdAndRestaurantId(customer.getId(), restaurant.getId())
                .or(() -> customer.getLoyaltyAccounts().stream()
                        .min(Comparator.comparing(LoyaltyAccount::getId)))
                .or(() -> Optional.of(createAccount(customer, restaurant)));
    }

    private String buildAccountNumber(Long customerId) {
        return String.format("ACCT-%04d", customerId);
    }

    private LoyaltyAccount createAccount(Customer customer, Restaurant restaurant) {
        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomer(customer);
        account.setRestaurant(restaurant);
        account.setAccountNumber(buildAccountNumber(customer.getId()));
        return loyaltyAccountRepository.save(account);
    }

    private Restaurant resolveOrCreateDefaultRestaurant() {
        return restaurantRepository.findByCode("DEMO")
                .orElseGet(() -> {
                    Restaurant restaurant = new Restaurant();
                    restaurant.setName("Demo Restaurant");
                    restaurant.setCode("DEMO");
                    restaurant.setActive(true);
                    restaurant.setDefaultCurrency("EUR");
                    return restaurantRepository.save(restaurant);
                });
    }
}
