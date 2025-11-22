package com.novareport.payments_xmr_service.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.HttpURLConnection;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class MoneroWalletClient {

    private final MeterRegistry meterRegistry;

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Value("${monero.wallet-rpc-url}")
    private String walletRpcUrl;

    @Value("${monero.min-confirmations:10}")
    private int minConfirmations;

    private void validateWalletRpcUrl(String url) {
        if (url == null || url.isBlank()) {
            throw new IllegalStateException("Monero wallet RPC URL is not configured");
        }
        try {
            URI parsed = URI.create(url);
            String protocol = parsed.getScheme();
            if (!"http".equals(protocol) && !"https".equals(protocol)) {
                throw new IllegalStateException("Invalid Monero wallet RPC URL protocol: " + protocol);
            }
            String host = parsed.getHost();
            if (!"localhost".equals(host) && !"127.0.0.1".equals(host) && !"monero-wallet-rpc".equals(host)) {
                throw new IllegalStateException("Invalid Monero wallet RPC URL host: " + host);
            }
        } catch (IllegalArgumentException e) {
            throw new IllegalStateException("Invalid Monero wallet RPC URL: " + url, e);
        }
    }

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
        params.put("in", Boolean.TRUE);
        params.put("account_index", accountIndex);
        params.put("subaddr_indices", List.of(subaddressIndex));

        Map<String, Object> result = invokeRpc("get_transfers", params);
        Object inTransfersValue = result.get("in");
        if (!(inTransfersValue instanceof List<?> inTransfers)) {
            return BigDecimal.ZERO;
        }

        BigDecimal totalAtomic = BigDecimal.ZERO;
        for (Object entry : inTransfers) {
            if (!(entry instanceof Map<?, ?> map)) {
                continue;
            }
            Object confirmationsValue = map.get("confirmations");
            Object amountValue = map.get("amount");
            if (!(confirmationsValue instanceof Number confirmations) || !(amountValue instanceof Number amount)) {
                continue;
            }
            if (confirmations.longValue() < minConfirmations) {
                continue;
            }

            BigDecimal atomic = new BigDecimal(amount.toString());
            totalAtomic = totalAtomic.add(atomic);
        }

        if (totalAtomic.compareTo(BigDecimal.ZERO) <= 0) {
            return BigDecimal.ZERO;
        }

        return totalAtomic.movePointLeft(12).setScale(12, RoundingMode.DOWN);
    }

    public int getMinConfirmations() {
        return minConfirmations;
    }

    public record MoneroSubaddress(int accountIndex, int subaddressIndex, String address) {
    }

    private Map<String, Object> invokeRpc(String method, Map<String, Object> params) {
        Timer.Sample sample = Timer.start(meterRegistry);
        String outcome = "error";
        try {
            Map<String, Object> request = new HashMap<>();
            request.put("jsonrpc", "2.0");
            request.put("id", "0");
            request.put("method", method);
            if (params != null && !params.isEmpty()) {
                request.put("params", params);
            }

            validateWalletRpcUrl(walletRpcUrl);

            String requestBody;
            try {
                requestBody = objectMapper.writeValueAsString(request);
            } catch (JsonProcessingException e) {
                throw new IllegalStateException("Failed to serialize Monero wallet RPC request", e);
            }

            log.info("Calling Monero wallet RPC. url={}, method={}, rawRequest={}", walletRpcUrl, method, requestBody);

            String responseBody;
            try {
                URI uri = URI.create(walletRpcUrl);
                HttpURLConnection connection = (HttpURLConnection) uri.toURL().openConnection();
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
            outcome = "success";
            return result;
        } catch (RuntimeException ex) {
            outcome = "error";
            throw ex;
        } finally {
            meterRegistry.counter("nova_monero_rpc_requests_total",
                    "method", method,
                    "outcome", outcome
            ).increment();
            sample.stop(
                    Timer.builder("nova_monero_rpc_latency_seconds")
                            .tag("method", method)
                            .tag("outcome", outcome)
                            .register(meterRegistry)
            );
        }
    }
}
