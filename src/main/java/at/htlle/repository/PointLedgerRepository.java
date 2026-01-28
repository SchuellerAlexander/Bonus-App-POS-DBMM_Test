package at.htlle.repository;

import at.htlle.entity.PointLedger;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PointLedgerRepository extends JpaRepository<PointLedger, Long> {

    @Query("select coalesce(sum(pl.points),0) from PointLedger pl where pl.loyaltyAccount.id = :accountId")
    Long sumPointsForAccount(@Param("accountId") Long accountId);

    List<PointLedger> findByLoyaltyAccountIdOrderByOccurredAtAsc(Long accountId);

    List<PointLedger> findByLoyaltyAccountIdOrderByOccurredAtDesc(Long accountId);

    @Query("select pl from PointLedger pl left join fetch pl.redemption "
            + "where pl.loyaltyAccount.id = :accountId order by pl.occurredAt desc, pl.id desc")
    List<PointLedger> findDetailedByAccountIdOrderByOccurredAtDesc(@Param("accountId") Long accountId);

    @Query(value = "select balance_after from point_ledger where loyalty_account_id = :accountId order by occurred_at desc, id desc limit 1",
            nativeQuery = true)
    Optional<Long> findLastBalanceForAccount(@Param("accountId") Long accountId);

    Page<PointLedger> findAllByOrderByOccurredAtDescIdDesc(Pageable pageable);
}
