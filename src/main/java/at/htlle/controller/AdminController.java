package at.htlle.controller;

import at.htlle.entity.Branch;
import at.htlle.entity.PointRule;
import at.htlle.entity.Restaurant;
import at.htlle.entity.Reward;
import at.htlle.entity.Customer;
import at.htlle.entity.LoyaltyAccount;
import at.htlle.entity.PointLedger;
import at.htlle.entity.Purchase;
import at.htlle.entity.Redemption;
import at.htlle.repository.BranchRepository;
import at.htlle.repository.CustomerRepository;
import at.htlle.repository.LoyaltyAccountRepository;
import at.htlle.repository.PointLedgerRepository;
import at.htlle.repository.PointRuleRepository;
import at.htlle.repository.PurchaseRepository;
import at.htlle.repository.RedemptionRepository;
import at.htlle.repository.RestaurantRepository;
import at.htlle.repository.RewardRepository;
import at.htlle.service.AdminManagementService;
import at.htlle.service.AdminRestaurantService;
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.HashMap;
import java.util.stream.Collectors;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/admin")
public class AdminController {

    private static final String DEFAULT_POINT_RULE_NAME = "Default Points";
    private static final String FIXED_ADMIN_USERNAME = "admin";

    private final RestaurantRepository restaurantRepository;
    private final BranchRepository branchRepository;
    private final RewardRepository rewardRepository;
    private final PointRuleRepository pointRuleRepository;
    private final CustomerRepository customerRepository;
    private final LoyaltyAccountRepository loyaltyAccountRepository;
    private final PointLedgerRepository pointLedgerRepository;
    private final PurchaseRepository purchaseRepository;
    private final RedemptionRepository redemptionRepository;
    private final AdminManagementService adminManagementService;
    private final AdminRestaurantService adminRestaurantService;

    public AdminController(RestaurantRepository restaurantRepository,
                           BranchRepository branchRepository,
                           RewardRepository rewardRepository,
                           PointRuleRepository pointRuleRepository,
                           CustomerRepository customerRepository,
                           LoyaltyAccountRepository loyaltyAccountRepository,
                           PointLedgerRepository pointLedgerRepository,
                           PurchaseRepository purchaseRepository,
                           RedemptionRepository redemptionRepository,
                           AdminManagementService adminManagementService,
                           AdminRestaurantService adminRestaurantService) {
        this.restaurantRepository = restaurantRepository;
        this.branchRepository = branchRepository;
        this.rewardRepository = rewardRepository;
        this.pointRuleRepository = pointRuleRepository;
        this.customerRepository = customerRepository;
        this.loyaltyAccountRepository = loyaltyAccountRepository;
        this.pointLedgerRepository = pointLedgerRepository;
        this.purchaseRepository = purchaseRepository;
        this.redemptionRepository = redemptionRepository;
        this.adminManagementService = adminManagementService;
        this.adminRestaurantService = adminRestaurantService;
    }

    @GetMapping
    public String adminHome(Model model) {
        model.addAttribute("customerCount", customerRepository.count());
        model.addAttribute("restaurantCount", restaurantRepository.count());
        model.addAttribute("pointsInCirculation", loyaltyAccountRepository.sumCurrentPoints());
        List<PointLedger> recentLedger = pointLedgerRepository
                .findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 8))
                .getContent();
        model.addAttribute("recentLedger", recentLedger);
        return "admin";
    }

    @GetMapping("/customers")
    public String customers(Model model) {
        List<Customer> customers = customerRepository.findAll().stream()
                .sorted(Comparator.comparing(Customer::getId))
                .toList();
        List<CustomerSummary> summaries = customers.stream()
                .map(customer -> new CustomerSummary(customer, primaryAccount(customer.getId())))
                .toList();
        model.addAttribute("customers", summaries);
        model.addAttribute("adminUsername", FIXED_ADMIN_USERNAME);
        return "admin-customers";
    }

    @PostMapping("/customers/{id}/role")
    public String updateRole(@PathVariable("id") Long customerId,
                             @RequestParam("role") Customer.Role role,
                             RedirectAttributes redirectAttributes) {
        adminManagementService.updateCustomerRole(customerId, role)
                .ifPresent(message -> redirectAttributes.addFlashAttribute("errorMessage", message));
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/status")
    public String updateStatus(@PathVariable("id") Long customerId,
                               @RequestParam("status") Customer.Status status,
                               RedirectAttributes redirectAttributes) {
        adminManagementService.updateCustomerStatus(customerId, status)
                .ifPresent(message -> redirectAttributes.addFlashAttribute("errorMessage", message));
        return "redirect:/admin/customers";
    }

    @PostMapping("/customers/{id}/delete")
    public String deleteCustomer(@PathVariable("id") Long customerId,
                                 RedirectAttributes redirectAttributes) {
        adminManagementService.deleteCustomer(customerId)
                .ifPresent(message -> redirectAttributes.addFlashAttribute("errorMessage", message));
        return "redirect:/admin/customers";
    }

    @GetMapping("/ledger")
    public String ledger(Model model) {
        List<PointLedger> entries = pointLedgerRepository
                .findAllByOrderByOccurredAtDescIdDesc(PageRequest.of(0, 200))
                .getContent();
        model.addAttribute("entries", entries);
        return "admin-ledger";
    }

    @PostMapping("/ledger/adjust")
    public String adjustPoints(@RequestParam("accountId") Long accountId,
                               @RequestParam("pointsDelta") Long pointsDelta,
                               @RequestParam("reason") String reason,
                               RedirectAttributes redirectAttributes) {
        adminManagementService.adjustPoints(accountId, pointsDelta, reason)
                .ifPresent(message -> redirectAttributes.addFlashAttribute("errorMessage", message));
        return "redirect:/admin/ledger";
    }

    @GetMapping("/purchases")
    public String purchases(Model model) {
        List<Purchase> purchases = purchaseRepository.findAllByOrderByPurchasedAtDesc();
        model.addAttribute("purchases", purchases);
        return "admin-purchases";
    }

    @GetMapping("/rewards")
    public String rewards(Model model) {
        List<Reward> rewards = rewardRepository.findAll().stream()
                .sorted(Comparator.comparing(Reward::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Restaurant> restaurants = restaurantRepository.findAll().stream()
                .sorted(Comparator.comparing(Restaurant::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Redemption> redemptions = redemptionRepository.findAllByOrderByRedeemedAtDesc();
        model.addAttribute("rewards", rewards);
        model.addAttribute("restaurants", restaurants);
        model.addAttribute("redemptions", redemptions);
        return "admin-rewards";
    }

    @GetMapping("/restaurants")
    public String restaurants(Model model) {
        loadAdminData(model);
        return "admin-restaurants";
    }

    @GetMapping("/restaurants/{id}/edit")
    public String editRestaurant(@PathVariable("id") Long restaurantId, Model model) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        PointRule rule = adminRestaurantService.findDefaultRule(restaurantId)
                .orElse(null);
        List<Reward> rewards = adminRestaurantService.listRewardsForRestaurant(restaurantId);
        model.addAttribute("restaurant", restaurant);
        model.addAttribute("pointRule", rule);
        model.addAttribute("rewards", rewards);
        model.addAttribute("rewardTypes", Reward.RewardType.values());
        return "admin-restaurant-edit";
    }

    @PostMapping("/restaurants/create")
    public String createRestaurant(@RequestParam("name") String name,
                                   @RequestParam("code") String code,
                                   @RequestParam(name = "active", defaultValue = "false") boolean active,
                                   @RequestParam(name = "defaultCurrency", defaultValue = "EUR") String defaultCurrency) {
        adminRestaurantService.createRestaurant(name, code, active, defaultCurrency);
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/restaurants/{id}/update")
    public String updateRestaurant(@PathVariable("id") Long restaurantId,
                                   @RequestParam("name") String name,
                                   @RequestParam("code") String code,
                                   @RequestParam(name = "active", defaultValue = "false") boolean active,
                                   @RequestParam(name = "defaultCurrency", defaultValue = "EUR") String defaultCurrency) {
        adminRestaurantService.updateRestaurant(restaurantId, name, code, active, defaultCurrency);
        return "redirect:/admin/restaurants/" + restaurantId + "/edit";
    }

    @PostMapping("/restaurants/{id}/delete")
    public String deleteRestaurant(@PathVariable("id") Long restaurantId) {
        adminRestaurantService.deleteRestaurant(restaurantId);
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/points-rules/set")
    public String setPointsRule(@RequestParam("restaurantId") Long restaurantId,
                                @RequestParam("pointsPerEuro") BigDecimal pointsPerEuro,
                                @RequestParam(name = "active", defaultValue = "true") boolean active) {
        adminRestaurantService.setDefaultPointRule(restaurantId, pointsPerEuro, active);
        return "redirect:/admin/restaurants/" + restaurantId + "/edit";
    }

    @PostMapping("/branches/create")
    public String createBranch(@RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("branchCode") String branchCode,
                               @RequestParam("name") String name,
                               @RequestParam(name = "defaultBranch", defaultValue = "false") boolean defaultBranch) {
        adminRestaurantService.createBranch(restaurantId, branchCode, name, defaultBranch);
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/branches/{id}/update")
    public String updateBranch(@PathVariable("id") Long branchId,
                               @RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("branchCode") String branchCode,
                               @RequestParam("name") String name,
                               @RequestParam(name = "defaultBranch", defaultValue = "false") boolean defaultBranch) {
        adminRestaurantService.updateBranch(branchId, restaurantId, branchCode, name, defaultBranch);
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/branches/{id}/default")
    public String setDefaultBranch(@PathVariable("id") Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        adminRestaurantService.updateBranch(
                branchId,
                branch.getRestaurant().getId(),
                branch.getBranchCode(),
                branch.getName(),
                true);
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/branches/{id}/delete")
    public String deleteBranch(@PathVariable("id") Long branchId) {
        adminRestaurantService.deleteBranch(branchId);
        return "redirect:/admin/restaurants";
    }

    @PostMapping("/rewards/create")
    public String createReward(@RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("rewardCode") String rewardCode,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("costPoints") Integer costPoints,
                               @RequestParam(name = "rewardType", defaultValue = "PRODUCT") Reward.RewardType rewardType,
                               @RequestParam(name = "active", defaultValue = "false") boolean active) {
        adminRestaurantService.createReward(
                restaurantId,
                rewardCode,
                name,
                description,
                costPoints,
                rewardType,
                active);
        return "redirect:/admin/restaurants/" + restaurantId + "/edit";
    }

    @PostMapping("/restaurants/{id}/rewards/create")
    public String createRestaurantReward(@PathVariable("id") Long restaurantId,
                                         @RequestParam("rewardCode") String rewardCode,
                                         @RequestParam("name") String name,
                                         @RequestParam("description") String description,
                                         @RequestParam("costPoints") Integer costPoints,
                                         @RequestParam(name = "rewardType", defaultValue = "PRODUCT") Reward.RewardType rewardType,
                                         @RequestParam(name = "active", defaultValue = "false") boolean active) {
        adminRestaurantService.createReward(
                restaurantId,
                rewardCode,
                name,
                description,
                costPoints,
                rewardType,
                active);
        return "redirect:/admin/restaurants/" + restaurantId + "/edit";
    }

    @PostMapping("/rewards/{id}/update")
    public String updateReward(@PathVariable("id") Long rewardId,
                               @RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("rewardCode") String rewardCode,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("costPoints") Integer costPoints,
                               @RequestParam(name = "rewardType", defaultValue = "PRODUCT") Reward.RewardType rewardType,
                               @RequestParam(name = "active", defaultValue = "false") boolean active) {
        adminRestaurantService.updateReward(
                rewardId,
                restaurantId,
                rewardCode,
                name,
                description,
                costPoints,
                rewardType,
                active);
        return "redirect:/admin/restaurants/" + restaurantId + "/edit";
    }

    @PostMapping("/rewards/{id}/delete")
    public String deleteReward(@PathVariable("id") Long rewardId,
                               @RequestParam("restaurantId") Long restaurantId) {
        adminRestaurantService.deleteReward(rewardId);
        return "redirect:/admin/restaurants/" + restaurantId + "/edit";
    }

    private void loadAdminData(Model model) {
        List<Restaurant> restaurants = adminRestaurantService.listRestaurants();
        List<Branch> branches = adminRestaurantService.listBranches();
        List<Reward> rewards = adminRestaurantService.listRewards();

        Map<Long, PointRule> defaultRules = new HashMap<>();
        for (Restaurant restaurant : restaurants) {
            if (restaurant == null || restaurant.getId() == null) {
                continue;
            }
            pointRuleRepository.findByRestaurantIdAndName(restaurant.getId(), DEFAULT_POINT_RULE_NAME)
                    .ifPresent(rule -> defaultRules.put(restaurant.getId(), rule));
        }

        model.addAttribute("restaurants", restaurants);
        model.addAttribute("branches", branches);
        model.addAttribute("rewards", rewards);
        model.addAttribute("defaultRules", defaultRules);
        model.addAttribute("branchesByRestaurant", branches.stream()
                .filter(branch -> branch.getRestaurant() != null && branch.getRestaurant().getId() != null)
                .collect(Collectors.groupingBy(branch -> branch.getRestaurant().getId())));
    }

    private LoyaltyAccount primaryAccount(Long customerId) {
        return loyaltyAccountRepository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .findFirst()
                .orElse(null);
    }

    private record CustomerSummary(Customer customer, LoyaltyAccount account) {
    }
}
