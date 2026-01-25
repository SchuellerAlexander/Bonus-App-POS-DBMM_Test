package at.htlle.repository;

import at.htlle.entity.LoyaltyAccount;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface LoyaltyAccountRepository extends JpaRepository<LoyaltyAccount, Long> {

    Optional<LoyaltyAccount> findByAccountNumber(String accountNumber);

    Optional<LoyaltyAccount> findByCustomerIdAndRestaurantId(Long customerId, Long restaurantId);

    List<LoyaltyAccount> findByCustomerIdOrderByIdAsc(Long customerId);

    @Query("select coalesce(sum(la.currentPoints),0) from LoyaltyAccount la")
    Long sumCurrentPoints();

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select la from LoyaltyAccount la where la.id = :id")
    Optional<LoyaltyAccount> lockById(@Param("id") Long id);
}
