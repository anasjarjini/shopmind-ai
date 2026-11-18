package com.shopmind.order;

import jakarta.persistence.LockModeType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface OrderRepository extends JpaRepository<ShopOrder, Long> {

    Page<ShopOrder> findByUserId(Long userId, Pageable pageable);

    Optional<ShopOrder> findByIdAndUserId(Long id, Long userId);

    /** Row lock for status changes (payment webhooks can arrive in parallel). */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select o from ShopOrder o where o.id = :id")
    Optional<ShopOrder> findByIdForUpdate(@Param("id") Long id);
}
