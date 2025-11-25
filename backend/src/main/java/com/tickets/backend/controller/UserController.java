package com.tickets.backend.controller;

import com.tickets.backend.dto.user.ManagedVenueDto;
import com.tickets.backend.dto.user.MeResponse;
import com.tickets.backend.dto.user.UpdateContactInfoRequest;
import com.tickets.backend.model.User;
import com.tickets.backend.service.CurrentUserService;
import com.tickets.backend.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class UserController {

    private final CurrentUserService currentUserService;
    private final UserService userService;

    public UserController(CurrentUserService currentUserService, UserService userService) {
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MeResponse> me() {
        User user = currentUserService.requireCurrentUser();
        List<String> roles = userService.getRoleNames(user);
        List<ManagedVenueDto> managedVenues = managedVenues(user.getId());
        return ResponseEntity.ok(buildResponse(user, roles, managedVenues));
    }

    @PutMapping("/me/contact-info")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<MeResponse> updateContactInfo(@Valid @RequestBody UpdateContactInfoRequest request) {
        User user = currentUserService.requireCurrentUser();
        User updated = userService.updateContactInfo(
            user.getId(),
            normalize(request.address()),
            normalize(request.phoneNumber())
        );
        List<String> roles = userService.getRoleNames(updated);
        List<ManagedVenueDto> managedVenues = managedVenues(updated.getId());
        return ResponseEntity.ok(buildResponse(updated, roles, managedVenues));
    }

    private List<ManagedVenueDto> managedVenues(UUID userId) {
        return userService.getManagedVenues(userId).stream()
            .map(venue -> new ManagedVenueDto(venue.getId(), venue.getName()))
            .toList();
    }

    private MeResponse buildResponse(User user, List<String> roles, List<ManagedVenueDto> managedVenues) {
        return new MeResponse(
            user.getId(),
            user.getEmail(),
            user.getDisplayName(),
            user.getAddress(),
            user.getPhoneNumber(),
            roles,
            managedVenues
        );
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
