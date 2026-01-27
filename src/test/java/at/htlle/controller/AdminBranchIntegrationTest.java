package at.htlle.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.flash;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import at.htlle.entity.Branch;
import at.htlle.entity.Restaurant;
import at.htlle.repository.BranchRepository;
import at.htlle.repository.RestaurantRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AdminBranchIntegrationTest {

    @Autowired
    private org.springframework.test.web.servlet.MockMvc mockMvc;

    @Autowired
    private RestaurantRepository restaurantRepository;

    @Autowired
    private BranchRepository branchRepository;

    @Test
    @WithMockUser(roles = "ADMIN")
    void createsBranchForRestaurant() throws Exception {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Branch Restaurant");
        restaurant.setCode("BRNCH");
        restaurant.setDefaultCurrency("EUR");
        restaurant.setActive(true);
        restaurantRepository.save(restaurant);

        mockMvc.perform(post("/admin/restaurants/{id}/branches", restaurant.getId())
                        .param("branchCode", "MAIN")
                        .param("name", "Main Branch")
                        .param("defaultBranch", "true")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection());

        Branch saved = branchRepository.findByRestaurantIdAndBranchCode(restaurant.getId(), "MAIN")
                .orElse(null);
        assertThat(saved).isNotNull();
        assertThat(saved.getRestaurant().getId()).isEqualTo(restaurant.getId());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsMissingRestaurant() throws Exception {
        mockMvc.perform(post("/admin/restaurants/{id}/branches", 9999L)
                        .param("branchCode", "NONE")
                        .param("name", "Missing Branch")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("branchError"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void rejectsDuplicateBranchCodePerRestaurant() throws Exception {
        Restaurant restaurant = new Restaurant();
        restaurant.setName("Dup Restaurant");
        restaurant.setCode("DUP");
        restaurant.setDefaultCurrency("EUR");
        restaurant.setActive(true);
        restaurantRepository.save(restaurant);

        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setBranchCode("DUP01");
        branch.setName("First");
        branchRepository.save(branch);

        mockMvc.perform(post("/admin/restaurants/{id}/branches", restaurant.getId())
                        .param("branchCode", "DUP01")
                        .param("name", "Second")
                        .with(csrf()))
                .andExpect(status().is3xxRedirection())
                .andExpect(flash().attributeExists("branchError"));
    }
}
