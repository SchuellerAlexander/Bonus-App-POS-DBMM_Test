package at.htlle.controller;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import at.htlle.dto.PurchaseRequest;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.PointRule;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.PointRuleRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.service.LoyaltyService;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class AdminControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private LoyaltyService loyaltyService;

    @Autowired
    private LoyaltyAccountRepository loyaltyAccountRepository;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private PointRuleRepository pointRuleRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void restaurantsPageLoadsWithoutError() throws Exception {
        mockMvc.perform(get("/admin/restaurants"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-restaurants"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void purchasesPageShowsRecordedPurchase() throws Exception {
        LoyaltyAccount account = loyaltyAccountRepository.findByAccountNumber("ACCT-0001")
                .orElseThrow();
        Long restaurantId = restaurantRepository.findAll().stream()
                .findFirst()
                .map(restaurant -> restaurant.getId())
                .orElseThrow();
        PointRule rule = pointRuleRepository.findAll().stream().findFirst().orElseThrow();

        String purchaseNumber = "PUR-" + UUID.randomUUID();
        PurchaseRequest purchaseRequest = new PurchaseRequest(
                account.getId(),
                restaurantId,
                purchaseNumber,
                BigDecimal.valueOf(15.25),
                "EUR",
                Instant.now(),
                "Test",
                "Admin dashboard visibility",
                rule.getId());
        loyaltyService.recordPurchase(purchaseRequest);

        mockMvc.perform(get("/admin/purchases"))
                .andExpect(status().isOk())
                .andExpect(view().name("admin-purchases"))
                .andExpect(content().string(containsString(purchaseNumber)));
    }
}
