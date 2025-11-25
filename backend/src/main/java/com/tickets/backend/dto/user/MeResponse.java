package com.tickets.backend.dto.user;

import java.util.List;
import java.util.UUID;

public record MeResponse(UUID id,
                         String email,
                         String displayName,
                         String address,
                         String phoneNumber,
                         List<String> roles,
                         List<ManagedVenueDto> managedVenues) {
}

