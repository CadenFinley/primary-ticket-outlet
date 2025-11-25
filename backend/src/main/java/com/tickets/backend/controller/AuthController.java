package com.tickets.backend.controller;

import com.tickets.backend.dto.auth.AuthResponse;
import com.tickets.backend.dto.auth.MockLoginRequest;
import com.tickets.backend.dto.auth.TokenPayload;
import com.tickets.backend.model.User;
import com.tickets.backend.service.AuthTokenService;
import com.tickets.backend.service.UserService;
import com.tickets.backend.service.VenueService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;
    private final VenueService venueService;
    private final AuthTokenService authTokenService;

    public AuthController(UserService userService,
                          VenueService venueService,
                          AuthTokenService authTokenService) {
        this.userService = userService;
        this.venueService = venueService;
        this.authTokenService = authTokenService;
    }

    @PostMapping("/mock")
    public ResponseEntity<AuthResponse> mockLogin(@Valid @RequestBody MockLoginRequest request) {
        User user = userService.findOrCreateUser(request.email(), request.displayName());
        userService.ensureRoles(user, request.roles());

        if (request.managedVenueIds() != null && !request.managedVenueIds().isEmpty()) {
            for (UUID venueId : request.managedVenueIds()) {
                userService.assignManagerToVenue(user, venueService.getById(venueId));
            }
        }

        List<String> roleNames = userService.getRoleNames(user);
        TokenPayload payload = new TokenPayload(user.getEmail(), user.getDisplayName(), roleNames);
        String token = authTokenService.generateToken(payload);
        AuthResponse response = new AuthResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getAddress(),
            user.getPhoneNumber(),
            roleNames,
            token
        );
        return ResponseEntity.ok(response);
    }
}
