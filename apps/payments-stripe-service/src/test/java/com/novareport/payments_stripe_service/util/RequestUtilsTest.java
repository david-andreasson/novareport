package com.novareport.payments_stripe_service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RequestUtilsTest {

    @Test
    void resolveUserIdReturnsUuidWhenAttributeIsValidString() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        UUID userId = UUID.randomUUID();
        when(request.getAttribute("uid")).thenReturn(userId.toString());

        UUID resolved = RequestUtils.resolveUserId(request);

        assertThat(resolved).isEqualTo(userId);
    }

    @Test
    void resolveUserIdThrowsWhenAttributeIsInvalidUuid() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("uid")).thenReturn("not-a-uuid");
        when(request.getRequestURI()).thenReturn("/api/test");

        assertThatThrownBy(() -> RequestUtils.resolveUserId(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid user ID");
    }

    @Test
    void resolveUserIdThrowsWhenAttributeMissing() {
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getAttribute("uid")).thenReturn(null);

        assertThatThrownBy(() -> RequestUtils.resolveUserId(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User ID not found");
    }
}
