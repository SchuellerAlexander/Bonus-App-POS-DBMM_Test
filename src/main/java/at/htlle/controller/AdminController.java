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
import java.math.BigDecimal;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
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

    public AdminController(RestaurantRepository restaurantRepository,
                           BranchRepository branchRepository,
                           RewardRepository rewardRepository,
                           PointRuleRepository pointRuleRepository,
                           CustomerRepository customerRepository,
                           LoyaltyAccountRepository loyaltyAccountRepository,
                           PointLedgerRepository pointLedgerRepository,
                           PurchaseRepository purchaseRepository,
                           RedemptionRepository redemptionRepository,
                           AdminManagementService adminManagementService) {
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

    @PostMapping("/restaurants/create")
    public String createRestaurant(@RequestParam("name") String name,
                                   @RequestParam("code") String code,
                                   @RequestParam(name = "active", defaultValue = "false") boolean active,
                                   @RequestParam(name = "defaultCurrency", defaultValue = "EUR") String defaultCurrency) {
        Restaurant restaurant = new Restaurant();
        restaurant.setName(name.trim());
        restaurant.setCode(code.trim().toUpperCase(Locale.ROOT));
        restaurant.setActive(active);
        restaurant.setDefaultCurrency(defaultCurrency.trim().toUpperCase(Locale.ROOT));
        restaurantRepository.save(restaurant);
        return "redirect:/admin";
    }

    @PostMapping("/restaurants/{id}/update")
    public String updateRestaurant(@PathVariable("id") Long restaurantId,
                                   @RequestParam("name") String name,
                                   @RequestParam("code") String code,
                                   @RequestParam(name = "active", defaultValue = "false") boolean active,
                                   @RequestParam(name = "defaultCurrency", defaultValue = "EUR") String defaultCurrency) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        restaurant.setName(name.trim());
        restaurant.setCode(code.trim().toUpperCase(Locale.ROOT));
        restaurant.setActive(active);
        restaurant.setDefaultCurrency(defaultCurrency.trim().toUpperCase(Locale.ROOT));
        restaurantRepository.save(restaurant);
        return "redirect:/admin";
    }

    @PostMapping("/points-rules/set")
    public String setPointsRule(@RequestParam("restaurantId") Long restaurantId,
                                @RequestParam("pointsPerEuro") BigDecimal pointsPerEuro,
                                @RequestParam(name = "active", defaultValue = "true") boolean active) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        PointRule rule = pointRuleRepository.findByRestaurantIdAndName(restaurantId, DEFAULT_POINT_RULE_NAME)
                .orElseGet(PointRule::new);
        rule.setRestaurant(restaurant);
        rule.setName(DEFAULT_POINT_RULE_NAME);
        rule.setRuleType(PointRule.RuleType.MULTIPLIER);
        rule.setMultiplier(pointsPerEuro);
        rule.setAmountThreshold(BigDecimal.ONE);
        rule.setBasePoints(0);
        rule.setActive(active);
        pointRuleRepository.save(rule);
        return "redirect:/admin";
    }

    @PostMapping("/branches/create")
    public String createBranch(@RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("branchCode") String branchCode,
                               @RequestParam("name") String name,
                               @RequestParam(name = "defaultBranch", defaultValue = "false") boolean defaultBranch) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        Branch branch = new Branch();
        branch.setRestaurant(restaurant);
        branch.setBranchCode(branchCode.trim().toUpperCase(Locale.ROOT));
        branch.setName(name.trim());
        branch.setDefaultBranch(defaultBranch);
        if (defaultBranch) {
            clearDefaultBranch(restaurantId);
        }
        branchRepository.save(branch);
        return "redirect:/admin";
    }

    @PostMapping("/branches/{id}/update")
    public String updateBranch(@PathVariable("id") Long branchId,
                               @RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("branchCode") String branchCode,
                               @RequestParam("name") String name,
                               @RequestParam(name = "defaultBranch", defaultValue = "false") boolean defaultBranch) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        branch.setRestaurant(restaurant);
        branch.setBranchCode(branchCode.trim().toUpperCase(Locale.ROOT));
        branch.setName(name.trim());
        branch.setDefaultBranch(defaultBranch);
        if (defaultBranch) {
            clearDefaultBranch(restaurantId);
        }
        branchRepository.save(branch);
        return "redirect:/admin";
    }

    @PostMapping("/branches/{id}/default")
    public String setDefaultBranch(@PathVariable("id") Long branchId) {
        Branch branch = branchRepository.findById(branchId)
                .orElseThrow(() -> new IllegalArgumentException("Branch not found"));
        clearDefaultBranch(branch.getRestaurant().getId());
        branch.setDefaultBranch(true);
        branchRepository.save(branch);
        return "redirect:/admin";
    }

    @PostMapping("/rewards/create")
    public String createReward(@RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("rewardCode") String rewardCode,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("costPoints") Integer costPoints,
                               @RequestParam(name = "active", defaultValue = "false") boolean active) {
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        Reward reward = new Reward();
        reward.setRestaurant(restaurant);
        reward.setRewardCode(rewardCode.trim().toUpperCase(Locale.ROOT));
        reward.setName(name.trim());
        reward.setDescription(description.trim());
        reward.setCostPoints(costPoints);
        reward.setActive(active);
        rewardRepository.save(reward);
        return "redirect:/admin";
    }

    @PostMapping("/rewards/{id}/update")
    public String updateReward(@PathVariable("id") Long rewardId,
                               @RequestParam("restaurantId") Long restaurantId,
                               @RequestParam("rewardCode") String rewardCode,
                               @RequestParam("name") String name,
                               @RequestParam("description") String description,
                               @RequestParam("costPoints") Integer costPoints,
                               @RequestParam(name = "active", defaultValue = "false") boolean active) {
        Reward reward = rewardRepository.findById(rewardId)
                .orElseThrow(() -> new IllegalArgumentException("Reward not found"));
        Restaurant restaurant = restaurantRepository.findById(restaurantId)
                .orElseThrow(() -> new IllegalArgumentException("Restaurant not found"));
        reward.setRestaurant(restaurant);
        reward.setRewardCode(rewardCode.trim().toUpperCase(Locale.ROOT));
        reward.setName(name.trim());
        reward.setDescription(description.trim());
        reward.setCostPoints(costPoints);
        reward.setActive(active);
        rewardRepository.save(reward);
        return "redirect:/admin";
    }

    private void clearDefaultBranch(Long restaurantId) {
        List<Branch> branches = branchRepository.findByRestaurantId(restaurantId);
        for (Branch branch : branches) {
            if (branch.isDefaultBranch()) {
                branch.setDefaultBranch(false);
                branchRepository.save(branch);
            }
        }
    }

    private void loadAdminData(Model model) {
        List<Restaurant> restaurants = safeList(restaurantRepository.findAll()).stream()
                .filter(restaurant -> restaurant.getId() != null)
                .sorted(Comparator.comparing(Restaurant::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Branch> branches = safeList(branchRepository.findAll()).stream()
                .filter(branch -> branch.getRestaurant() != null)
                .sorted(Comparator.comparing(Branch::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();
        List<Reward> rewards = safeList(rewardRepository.findAll()).stream()
                .filter(reward -> reward.getRestaurant() != null)
                .sorted(Comparator.comparing(Reward::getName, Comparator.nullsLast(String::compareToIgnoreCase)))
                .toList();

        Map<Long, PointRule> defaultRules = restaurants.stream()
                .map(restaurant -> Map.entry(
                        restaurant.getId(),
                        pointRuleRepository.findByRestaurantIdAndName(restaurant.getId(), DEFAULT_POINT_RULE_NAME)
                                .orElse(null)))
                .filter(entry -> entry.getKey() != null && entry.getValue() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        model.addAttribute("restaurants", restaurants);
        model.addAttribute("branches", branches);
        model.addAttribute("rewards", rewards);
        model.addAttribute("defaultRules", defaultRules);
    }

    private <T> List<T> safeList(List<T> items) {
        return items == null ? List.of() : items;
    }

    private LoyaltyAccount primaryAccount(Long customerId) {
        return loyaltyAccountRepository.findByCustomerIdOrderByIdAsc(customerId).stream()
                .findFirst()
                .orElse(null);
    }

    private record CustomerSummary(Customer customer, LoyaltyAccount account) {
    }
}
