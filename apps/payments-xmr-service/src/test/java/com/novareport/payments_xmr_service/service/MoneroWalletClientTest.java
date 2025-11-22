package com.novareport.payments_xmr_service.service;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.springframework.test.util.ReflectionTestUtils;

@SuppressWarnings("null")
class MoneroWalletClientTest {

    private final SimpleMeterRegistry meterRegistry = new SimpleMeterRegistry();
    private final MoneroWalletClient client = new MoneroWalletClient(meterRegistry);

    @Test
    void getConfirmedBalanceThrowsWhenWalletRpcUrlNotConfigured() {
        assertThatThrownBy(() -> client.getConfirmedBalanceForSubaddress(0, 1))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Monero wallet RPC URL is not configured");
    }

    @Test
    void getMinConfirmationsReturnsConfiguredValue() {
        ReflectionTestUtils.setField(client, "minConfirmations", 5);

        assertThat(client.getMinConfirmations()).isEqualTo(5);
    }

    @Test
    void validateWalletRpcUrlRejectsNullOrBlank() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(client, "validateWalletRpcUrl", (Object) null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("not configured");

        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(client, "validateWalletRpcUrl", "  "))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void validateWalletRpcUrlRejectsInvalidProtocol() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(client, "validateWalletRpcUrl", "ftp://localhost:18082"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("protocol");
    }

    @Test
    void validateWalletRpcUrlRejectsInvalidHost() {
        assertThatThrownBy(() -> ReflectionTestUtils.invokeMethod(client, "validateWalletRpcUrl", "http://evil.com:18082"))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("host");
    }

    @Test
    void validateWalletRpcUrlAcceptsLocalAndMoneroHosts() {
        ReflectionTestUtils.invokeMethod(client, "validateWalletRpcUrl", "http://localhost:18082/json_rpc");
        ReflectionTestUtils.invokeMethod(client, "validateWalletRpcUrl", "http://127.0.0.1:18082/json_rpc");
        ReflectionTestUtils.invokeMethod(client, "validateWalletRpcUrl", "http://monero-wallet-rpc:18082/json_rpc");
    }
}
