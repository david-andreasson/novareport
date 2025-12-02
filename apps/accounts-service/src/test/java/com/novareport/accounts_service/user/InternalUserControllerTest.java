package com.novareport.accounts_service.user;

import com.novareport.accounts_service.user.dto.UserProfileResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class InternalUserControllerTest {

    @Mock
    private UserRepository userRepository;

    private InternalUserController controller;

    @BeforeEach
    void setUp() {
        controller = new InternalUserController(userRepository);
    }

    @Test
    void getUserProfileReturnsUserWhenFound() {
        UUID userId = UUID.randomUUID();
        User user = createTestUser(userId);
        when(userRepository.findById(userId)).thenReturn(Optional.of(user));

        UserProfileResponse response = controller.getUserProfile(userId);

        assertThat(response).isNotNull();
        assertThat(response.id()).isEqualTo(userId);
        assertThat(response.email()).isEqualTo("test@example.com");
        assertThat(response.firstName()).isEqualTo("John");
    }

    @Test
    void getUserProfileThrowsWhenUserNotFound() {
        UUID userId = UUID.randomUUID();
        when(userRepository.findById(userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> controller.getUserProfile(userId))
                .isInstanceOf(NoSuchElementException.class)
                .hasMessageContaining("User not found");
    }

    private User createTestUser(UUID userId) {
        User user = new User();
        user.setId(userId);
        user.setEmail("test@example.com");
        user.setFirstName("John");
        user.setLastName("Doe");
        user.setRole("USER");
        user.setIsActive(true);
        user.setCreatedAt(Instant.now());
        return user;
    }
}
