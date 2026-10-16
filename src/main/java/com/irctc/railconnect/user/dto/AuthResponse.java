package com.irctc.railconnect.user.dto;

import com.irctc.railconnect.user.domain.UserRole;

public record AuthResponse(
        String accessToken,
        String tokenType,
        long expiresInSeconds,
        Long userId,
        String fullName,
        String email,
        UserRole role
) {
    public static AuthResponse bearer(
            String accessToken,
            long expiresInSeconds,
            Long userId,
            String fullName,
            String email,
            UserRole role
    ) {
        return new AuthResponse(accessToken, "Bearer", expiresInSeconds, userId, fullName, email, role);
    }
}
