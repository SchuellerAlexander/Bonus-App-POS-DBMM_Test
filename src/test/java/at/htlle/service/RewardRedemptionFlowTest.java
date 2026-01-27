package at.htlle.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import at.htlle.entity.Customer;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.Restaurant;
import at.htlle.entity.Reward;
import at.htlle.entity.RewardRedemption;
import at.htlle.repository.CustomerRepository;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRedemptionRepository;
import at.htlle.repository.RewardRepository;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@Transactional
class RewardRedemptionFlowTest {

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private CustomerRepository customerRepository;

    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Autowired
    private RewardRepository rewardRepository;

    @Autowired
    private RewardRedemptionRepository rewardRedemptionRepository;

    @Test
    void redeemsRewardAndGeneratesCode() {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Test Restaurant");
        restaurant.setCode("TEST");
        restaurant.setDefaultCurrency("EUR");
        restaurant.setActive(true);
        restaurantRepository.save(restaurant);

        Customer customer = new Customer();
        customer.setFirstName("Test");
        customer.setLastName("User");
        customer.setEmail("test-" + UUID.randomUUID() + "@example.com");
        customer.setUsername("user-" + UUID.randomUUID());
        customer.setPassword("secret");
        customerRepository.save(customer);

        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomer(customer);
        account.setRestaurant(restaurant);
        account.setAccountNumber("ACCT-" + customer.getId());
        account.setCurrentPoints(120L);
        loyaltyAccountRepository.save(account);

        Reward reward = new Reward();
        reward.setRestaurant(restaurant);
        reward.setRewardCode("FREE-DRINK");
        reward.setName("Free Drink");
        reward.setDescription("Test reward");
        reward.setCostPoints(50);
        reward.setActive(true);
        rewardRepository.save(reward);

        RewardRedemption redemption = loyaltyService.redeemReward(account.getId(), reward.getId(), "Enjoy!");

        assertThat(redemption.getRedemptionCode()).isNotBlank();
        assertThat(redemption.isRedeemed()).isFalse();
        assertThat(rewardRedemptionRepository.findByRedemptionCode(redemption.getRedemptionCode()))
                .isPresent();

        LoyaltyAccount updated = loyaltyAccountRepository.findById(account.getId()).orElseThrow();
        assertThat(updated.getCurrentPoints()).isEqualTo(70L);
    }

    @Test
    void preventsDoubleRedemption() {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Redeem Restaurant");
        restaurant.setCode("RDM");
        restaurant.setDefaultCurrency("EUR");
        restaurant.setActive(true);
        restaurantRepository.save(restaurant);

        Customer customer = new Customer();
        customer.setFirstName("Redeem");
        customer.setLastName("Tester");
        customer.setEmail("redeem-" + UUID.randomUUID() + "@example.com");
        customer.setUsername("redeem-" + UUID.randomUUID());
        customer.setPassword("secret");
        customerRepository.save(customer);

        LoyaltyAccount account = new LoyaltyAccount();
        account.setCustomer(customer);
        account.setRestaurant(restaurant);
        account.setAccountNumber("ACCT-" + customer.getId());
        account.setCurrentPoints(200L);
        loyaltyAccountRepository.save(account);

        Reward reward = new Reward();
        reward.setRestaurant(restaurant);
        reward.setRewardCode("SNACK");
        reward.setName("Snack");
        reward.setDescription("Test reward");
        reward.setCostPoints(30);
        reward.setActive(true);
        rewardRepository.save(reward);

        RewardRedemption redemption = loyaltyService.redeemReward(account.getId(), reward.getId(), null);
        loyaltyService.redeemByCode(redemption.getRedemptionCode());

        assertThatThrownBy(() -> loyaltyService.redeemByCode(redemption.getRedemptionCode()))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("already used");
    }
}
