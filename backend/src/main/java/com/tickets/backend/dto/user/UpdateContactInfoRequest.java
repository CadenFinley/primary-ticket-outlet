package com.tickets.backend.dto.user;

import jakarta.validation.constraints.Size;

public record UpdateContactInfoRequest(
    @Size(max = 512)
    String address,
    @Size(max = 64)
    String phoneNumber
) {
}
