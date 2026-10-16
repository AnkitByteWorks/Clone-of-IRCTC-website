package com.irctc.railconnect.user.security;

import com.irctc.railconnect.user.domain.UserRole;

public record AuthenticatedUser(
        Long userId,
        String email,
        UserRole role
) {}
