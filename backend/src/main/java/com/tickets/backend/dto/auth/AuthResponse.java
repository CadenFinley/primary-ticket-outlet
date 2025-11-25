package com.tickets.backend.dto.auth;

import java.util.List;
import java.util.UUID;

public record AuthResponse(
    UUID userId,
    String email,
    String displayName,
    String address,
    String phoneNumber,
    List<String> roles,
    String token
) {
}

