package at.htlle.repository;

import at.htlle.entity.Branch;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface BranchRepository extends JpaRepository<Branch, Long> {
    Optional<Branch> findByRestaurantIdAndBranchCode(Long restaurantId, String branchCode);

    List<Branch> findByRestaurantId(Long restaurantId);
}
