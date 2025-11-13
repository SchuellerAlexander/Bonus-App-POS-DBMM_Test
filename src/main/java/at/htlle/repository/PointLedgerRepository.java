package at.htlle.repository;

import at.htlle.entity.PointLedger;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {

    @Query("select coalesce(sum(pl.points),0) from PointLedger pl where pl.loyaltyAccount.id = :accountId")
    Long sumPointsForAccount(@Param("accountId") Long accountId);

    List<PointLedger> findByLoyaltyAccountIdOrderByOccurredAtAsc(Long accountId);
}
