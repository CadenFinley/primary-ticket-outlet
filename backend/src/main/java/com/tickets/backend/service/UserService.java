package com.tickets.backend.service;

import com.tickets.backend.model.Role;
import com.tickets.backend.model.User;
import com.tickets.backend.model.UserRole;
import com.tickets.backend.model.Venue;
import com.tickets.backend.repository.RoleRepository;
import com.tickets.backend.repository.UserRepository;
import com.tickets.backend.repository.UserRoleRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class UserService {

    private static final Map<String, String> ROLE_ALIASES = Map.of(
        "user", "ROLE_USER",
        "manager", "ROLE_MANAGER",
        "admin", "ROLE_ADMIN"
    );

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    public UserService(UserRepository userRepository,
                       RoleRepository roleRepository,
                       UserRoleRepository userRoleRepository) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.userRoleRepository = userRoleRepository;
    }

    @Transactional
    public User findOrCreateUser(String email, String displayName) {
        return userRepository.findWithRolesByEmailIgnoreCase(email)
            .map(user -> updateDisplayNameIfNeeded(user, displayName))
            .orElseGet(() -> createUser(email, displayName));
    }

    @Transactional
    public void ensureRoles(User user, Collection<String> rawRoles) {
        Collection<String> rolesToProcess = rawRoles == null ? List.of() : rawRoles;

        Set<UserRole> userRoles = Optional.ofNullable(user.getUserRoles())
            .orElseGet(() -> {
                Set<UserRole> initialized = new HashSet<>();
                user.setUserRoles(initialized);
                return initialized;
            });

        Set<String> normalizedRoles = rolesToProcess.stream()
            .map(this::normalizeRoleName)
            .filter(Optional::isPresent)
            .map(Optional::get)
            .collect(Collectors.toCollection(LinkedHashSet::new));

        if (normalizedRoles.isEmpty()) {
            normalizedRoles.add("ROLE_USER");
        }

        for (String roleName : normalizedRoles) {
            Role role = roleRepository.findByNameIgnoreCase(roleName)
                .orElseGet(() -> roleRepository.save(Role.builder().name(roleName).build()));
            boolean alreadyAssigned = userRoles.stream()
                .map(UserRole::getRole)
                .filter(Objects::nonNull)
                .anyMatch(existingRole -> roleName.equalsIgnoreCase(existingRole.getName()))
                || (user.getId() != null && userRoleRepository.existsGlobalRole(user.getId(), roleName));
            if (!alreadyAssigned) {
                UserRole newAssignment = UserRole.builder()
                    .user(user)
                    .role(role)
                    .build();
                userRoles.add(newAssignment);
                if (user.getId() != null) {
                    userRoleRepository.save(newAssignment);
                }
            }
        }

        userRepository.save(user);
    }

    public List<String> getRoleNames(User user) {
        return Optional.ofNullable(user.getUserRoles())
            .orElseGet(Collections::emptySet).stream()
            .map(UserRole::getRole)
            .filter(Objects::nonNull)
            .map(Role::getName)
            .filter(Objects::nonNull)
            .distinct()
            .toList();
    }

    public List<Venue> getManagedVenues(UUID userId) {
        return userRoleRepository.findManagerVenues(userId);
    }

    @Transactional
    public void assignManagerToVenue(User user, Venue venue) {
        Role managerRole = roleRepository.findByNameIgnoreCase("ROLE_MANAGER")
            .orElseGet(() -> roleRepository.save(Role.builder().name("ROLE_MANAGER").build()));

        boolean exists = userRoleRepository
            .findManagerAssignment(user.getId(), managerRole.getName(), venue.getId())
            .isPresent();
        if (!exists) {
            userRoleRepository.save(UserRole.builder()
                .user(user)
                .role(managerRole)
                .venue(venue)
                .build());
        }
    }

    @Transactional
    public User updateContactInfo(UUID userId, String address, String phoneNumber) {
        User user = userRepository.findById(userId)
            .orElseThrow(() -> new IllegalArgumentException("User not found"));
        user.setAddress(address);
        user.setPhoneNumber(phoneNumber);
        return userRepository.save(user);
    }

    private User createUser(String email, String displayName) {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(email)
            .displayName(displayName)
            .build();
        return userRepository.save(user);
    }

    private User updateDisplayNameIfNeeded(User user, String displayName) {
        if (displayName != null && !displayName.isBlank() && !displayName.equals(user.getDisplayName())) {
            user.setDisplayName(displayName);
        }
        return user;
    }

    private Optional<String> normalizeRoleName(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        String trimmed = value.trim();
        if (trimmed.toUpperCase().startsWith("ROLE_")) {
            return Optional.of(trimmed.toUpperCase());
        }
        String alias = ROLE_ALIASES.get(trimmed.toLowerCase());
        return Optional.ofNullable(alias);
    }
}
