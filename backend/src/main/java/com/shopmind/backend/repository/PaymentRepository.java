package com.shopmind.backend.repository;

import com.shopmind.backend.entity.Payment;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import java.util.Optional;
import java.util.UUID;

public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    /** Locked: two identical webhooks arriving together must be processed one after the other. */
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Payment> findWithLockByProviderRef(String providerRef);
}
