package at.htlle.repository;

import at.htlle.entity.RewardRedemption;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RewardRedemptionRepository extends JpaRepository<RewardRedemption, Long> {
    Optional<RewardRedemption> findByRedemptionCode(String redemptionCode);

    boolean existsByRedemptionCode(String redemptionCode);

    List<RewardRedemption> findAllByOrderByCreatedAtDesc();
}
