package com.novareport.payments_xmr_service.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

@Entity
@Table(name = "payments")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @UuidGenerator
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "payment_address", nullable = false)
    private String paymentAddress;

    @Column(name = "amount_xmr", nullable = false, precision = 19, scale = 8)
    private BigDecimal amountXmr;

    @Column(name = "plan", nullable = false, length = 50)
    private String plan;

    @Column(name = "duration_days", nullable = false)
    private Integer durationDays;

    @Column(name = "wallet_account_index")
    private Integer walletAccountIndex;

    @Column(name = "wallet_subaddress_index")
    private Integer walletSubaddressIndex;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private PaymentStatus status = PaymentStatus.PENDING;

    @Column(nullable = false, name = "created_at")
    @Builder.Default
    private Instant createdAt = Instant.now();

    @Column(name = "confirmed_at")
    private Instant confirmedAt;

    public boolean isPending() {
        return status == PaymentStatus.PENDING;
    }

    public void confirm() {
        this.status = PaymentStatus.CONFIRMED;
        this.confirmedAt = Instant.now();
    }

    public void fail() {
        this.status = PaymentStatus.FAILED;
        this.confirmedAt = Instant.now();
    }
}
