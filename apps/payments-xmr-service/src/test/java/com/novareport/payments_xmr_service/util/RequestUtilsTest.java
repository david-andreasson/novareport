package com.novareport.payments_xmr_service.util;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RequestUtilsTest {

    @Mock
    private HttpServletRequest request;

    @Test
    void resolveUserIdReturnsUuidWhenAttributeIsValid() {
        UUID userId = UUID.randomUUID();
        when(request.getAttribute("uid")).thenReturn(userId.toString());

        UUID resolved = RequestUtils.resolveUserId(request);

        assertThat(resolved).isEqualTo(userId);
    }

    @Test
    void resolveUserIdThrowsWhenAttributeIsMissing() {
        when(request.getAttribute("uid")).thenReturn(null);

        assertThatThrownBy(() -> RequestUtils.resolveUserId(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("User ID not found");
    }

    @Test
    void resolveUserIdThrowsWhenAttributeIsInvalidUuid() {
        when(request.getAttribute("uid")).thenReturn("not-a-uuid");
        when(request.getRequestURI()).thenReturn("/api/v1/test");

        assertThatThrownBy(() -> RequestUtils.resolveUserId(request))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("Invalid user ID");
    }
}
