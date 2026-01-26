package at.htlle.repository;

import at.htlle.entity.Purchase;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PurchaseRepository extends JpaRepository<Purchase, Long> {
    Optional<Purchase> findByPurchaseNumber(String purchaseNumber);

    List<Purchase> findAllByOrderByPurchasedAtDesc();
}
