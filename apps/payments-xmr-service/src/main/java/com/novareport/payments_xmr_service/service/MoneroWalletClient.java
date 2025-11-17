package com.novareport.payments_xmr_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
        Map<String, Object> params = new HashMap<>();
        params.put("account_index", accountIndex);
        if (label != null && !label.isBlank()) {
            params.put("label", label);
        }

        Map<String, Object> result = invokeRpc("create_address", params);
        Object addressValue = result.get("address");
        Object indexValue = result.get("address_index");

        if (!(addressValue instanceof String address) || !(indexValue instanceof Number index)) {
            throw new IllegalStateException("Invalid response from Monero wallet RPC create_address");
        }

        return new MoneroSubaddress(accountIndex, index.intValue(), address);
    }

    public BigDecimal getConfirmedBalanceForSubaddress(int accountIndex, int subaddressIndex) {
        Map<String, Object> params = new HashMap<>();
        params.put("account_index", accountIndex);
        params.put("address_indices", List.of(subaddressIndex));

        Map<String, Object> result = invokeRpc("get_balance", params);
        Object perSubaddressValue = result.get("per_subaddress");
        if (!(perSubaddressValue instanceof List<?> perSubaddressList)) {
            return BigDecimal.ZERO;
        }

        for (Object entry : perSubaddressList) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            Object indexValue = map.get("address_index");
            Object unlockedBalanceValue = map.get("unlocked_balance");
            if (!(indexValue instanceof Number index) || !(unlockedBalanceValue instanceof Number unlockedBalance)) {
                continue;
            }
            if (index.intValue() != subaddressIndex) {
                continue;
            }

            BigDecimal atomic = new BigDecimal(unlockedBalance.toString());
            return atomic.movePointLeft(12).setScale(12, RoundingMode.DOWN);
        }

        return BigDecimal.ZERO;
    }

    public int getMinConfirmations() {
        return minConfirmations;
    }

    public record MoneroSubaddress(int accountIndex, int subaddressIndex, String address) {
    }

    private Map<String, Object> invokeRpc(String method, Map<String, Object> params) {
        Map<String, Object> request = new HashMap<>();
        request.put("jsonrpc", "2.0");
        request.put("id", "0");
        request.put("method", method);
        if (params != null && !params.isEmpty()) {
            request.put("params", params);
        }

        Map<?, ?> response;
        try {
            if (walletRpcUrl == null || walletRpcUrl.isBlank()) {
                throw new IllegalStateException("Monero wallet RPC URL is not configured");
            }
            response = restTemplate.postForObject(walletRpcUrl, request, Map.class);
        } catch (RestClientException e) {
            throw new IllegalStateException("Failed to call Monero wallet RPC", e);
        }

        if (response == null) {
            throw new IllegalStateException("Empty response from Monero wallet RPC");
        }

        Object errorValue = response.get("error");
        if (errorValue instanceof Map<?, ?> errorMap) {
            Object message = errorMap.get("message");
            throw new IllegalStateException("Monero wallet RPC error: " + message);
        }

        Object resultValue = response.get("result");
        if (!(resultValue instanceof Map<?, ?>)) {
            throw new IllegalStateException("Invalid result from Monero wallet RPC");
        }

        @SuppressWarnings("unchecked")
        Map<String, Object> result = (Map<String, Object>) resultValue;
        return result;
    }
}
