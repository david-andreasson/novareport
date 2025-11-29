package com.novareport.accounts_service.user;

import com.novareport.accounts_service.user.dto.UserProfileResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.lang.NonNull;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/accounts/internal")
@RequiredArgsConstructor
public class InternalUserController {

    private final UserRepository users;

    @GetMapping("/users/{userId}/profile")
    public UserProfileResponse getUserProfile(@PathVariable @NonNull UUID userId) {
        var user = users.findById(userId)
            .orElseThrow(() -> new java.util.NoSuchElementException("User not found with id: " + userId));
        return new UserProfileResponse(
            user.getId(),
            user.getEmail(),
            user.getFirstName(),
            user.getLastName(),
            user.getRole()
        );
    }
}
