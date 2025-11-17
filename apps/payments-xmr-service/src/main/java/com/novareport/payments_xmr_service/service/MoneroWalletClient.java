package com.novareport.payments_xmr_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoneroWalletClient {

    private final RestTemplate restTemplate;

    private final ObjectMapper objectMapper = new ObjectMapper();

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

        if (walletRpcUrl == null || walletRpcUrl.isBlank()) {
            throw new IllegalStateException("Monero wallet RPC URL is not configured");
        }

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(request);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialize Monero wallet RPC request", e);
        }

        log.info("Calling Monero wallet RPC. url={}, method={}, rawRequest={}", walletRpcUrl, method, requestBody);

        String responseBody;
        try {
            URL url = new URL(walletRpcUrl);
            HttpURLConnection connection = (HttpURLConnection) url.openConnection();
            connection.setRequestMethod("POST");
            connection.setRequestProperty("Content-Type", "application/json");
            connection.setDoOutput(true);

            byte[] bytes = requestBody.getBytes(StandardCharsets.UTF_8);
            connection.setFixedLengthStreamingMode(bytes.length);
            connection.getOutputStream().write(bytes);

            int status = connection.getResponseCode();
            InputStream inputStream = status >= 400 ? connection.getErrorStream() : connection.getInputStream();
            if (inputStream == null) {
                throw new IllegalStateException("Empty response stream from Monero wallet RPC, HTTP status=" + status);
            }
            responseBody = new String(inputStream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (MalformedURLException e) {
            throw new IllegalStateException("Invalid Monero wallet RPC URL: " + walletRpcUrl, e);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to call Monero wallet RPC", e);
        }

        log.info("Monero wallet RPC raw response for method {}: {}", method, responseBody);

        Map<?, ?> response;
        try {
            response = objectMapper.readValue(responseBody, Map.class);
        } catch (IOException e) {
            throw new IllegalStateException("Failed to parse Monero wallet RPC response", e);
        }

        if (response == null) {
            throw new IllegalStateException("Empty response from Monero wallet RPC");
        }

        Object errorValue = response.get("error");
        if (errorValue instanceof Map<?, ?> errorMap) {
            Object code = errorMap.get("code");
            Object message = errorMap.get("message");
            log.error("Monero wallet RPC returned error. method={}, code={}, message={}, response={}", method, code, message, responseBody);
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
