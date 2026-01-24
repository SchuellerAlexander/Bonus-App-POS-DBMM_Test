package at.htlle.repository;

import at.htlle.entity.PointRule;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointRuleRepository extends JpaRepository<PointRule, Long> {

    Optional<PointRule> findByRestaurantIdAndName(Long restaurantId, String name);

    List<PointRule> findByRestaurantId(Long restaurantId);

    @Query("select pr from PointRule pr where pr.restaurant.id = :restaurantId and pr.active = true and (pr.validFrom is null or pr.validFrom <= :referenceDate) and (pr.validUntil is null or pr.validUntil >= :referenceDate)")
    List<PointRule> findActiveRulesForDate(@Param("restaurantId") Long restaurantId, @Param("referenceDate") LocalDate referenceDate);
}
