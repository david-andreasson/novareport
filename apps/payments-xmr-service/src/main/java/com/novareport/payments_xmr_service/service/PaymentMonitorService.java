package com.novareport.payments_xmr_service.service;

import com.novareport.payments_xmr_service.domain.Payment;
import com.novareport.payments_xmr_service.domain.PaymentRepository;
import com.novareport.payments_xmr_service.domain.PaymentStatus;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentMonitorService {

    private final PaymentRepository paymentRepository;
    private final MoneroWalletClient moneroWalletClient;
    private final PaymentService paymentService;

    @Value("${payments.fake-mode:true}")
    private boolean fakeMode;

    @Value("${payments.monitor-enabled:true}")
    private boolean monitorEnabled;

    @Scheduled(fixedDelayString = "${payments.monitor-interval-ms:60000}")
    public void checkPendingPayments() {
        if (fakeMode || !monitorEnabled) {
            return;
        }

        try {
            moneroWalletClient.refresh();
        } catch (Exception e) {
            log.warn("Failed to refresh Monero wallet", e);
        }

        List<Payment> pendingPayments = paymentRepository.findByStatus(PaymentStatus.PENDING);
        if (pendingPayments.isEmpty()) {
            return;
        }

        for (Payment payment : pendingPayments) {
            Integer accountIndex = payment.getWalletAccountIndex();
            Integer subaddressIndex = payment.getWalletSubaddressIndex();
            if (accountIndex == null || subaddressIndex == null) {
                continue;
            }

            try {
                BigDecimal confirmedBalance = moneroWalletClient.getConfirmedBalanceForSubaddress(accountIndex, subaddressIndex);
                if (confirmedBalance.compareTo(payment.getAmountXmr()) >= 0) {
                    paymentService.confirmPayment(payment.getId());
                }
            } catch (Exception e) {
                log.warn("Failed to check or confirm payment {}", payment.getId(), e);
            }
        }
    }
}
