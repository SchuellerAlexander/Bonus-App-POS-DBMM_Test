package at.htlle.repository;

import at.htlle.entity.Redemption;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RedemptionRepository extends JpaRepository<Redemption, Long> {

    List<Redemption> findByLoyaltyAccountIdOrderByRedeemedAtDesc(Long loyaltyAccountId);
}
