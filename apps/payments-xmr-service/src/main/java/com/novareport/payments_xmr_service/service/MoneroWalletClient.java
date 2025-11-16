package com.novareport.payments_xmr_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoneroWalletClient {

    private final RestTemplate restTemplate;

    @Value("${monero.wallet-rpc-url}")
    private String walletRpcUrl;

    @Value("${monero.min-confirmations:10}")
    private int minConfirmations;

    public MoneroSubaddress createSubaddress(int accountIndex, String label) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public BigDecimal getConfirmedBalanceForSubaddress(int accountIndex, int subaddressIndex) {
        throw new UnsupportedOperationException("Not implemented yet");
    }

    public int getMinConfirmations() {
        return minConfirmations;
    }

    public record MoneroSubaddress(int accountIndex, int subaddressIndex, String address) {
    }
}
