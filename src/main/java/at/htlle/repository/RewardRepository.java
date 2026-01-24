package at.htlle.repository;

import at.htlle.entity.Reward;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRepository extends JpaRepository<Reward, Long> {

    Optional<Reward> findByRestaurantIdAndRewardCode(Long restaurantId, String rewardCode);

    List<Reward> findByRestaurantIdAndActiveTrue(Long restaurantId);

    List<Reward> findByRestaurantId(Long restaurantId);
}
