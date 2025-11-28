package com.novareport.payments_xmr_service.domain;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, UUID> {

    Optional<Payment> findByIdAndUserId(UUID id, UUID userId);
    
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM Payment p WHERE p.id = :id")
    Optional<Payment> findByIdWithLock(@Param("id") UUID id);

    List<Payment> findByStatus(PaymentStatus status);

    Optional<Payment> findTop1ByUserIdOrderByCreatedAtDesc(UUID userId);

    @Override
    @NonNull
    <S extends Payment> S save(@NonNull S entity);
}
