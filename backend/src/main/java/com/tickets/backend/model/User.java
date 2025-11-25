package com.tickets.backend.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Builder.Default;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.RequiredArgsConstructor;
import lombok.ToString;

import java.time.Instant;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Data
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@RequiredArgsConstructor(access = AccessLevel.PROTECTED)
@EqualsAndHashCode(of = "id")
@ToString(of = {"id", "email", "displayName"})
@Entity
@Table(name = "users")
public class User {

    @Id
    private UUID id;

    private String email;

    private String displayName;

    private String address;

    @Column(name = "phone_number")
    private String phoneNumber;

    @Default
    private Instant createdAt = Instant.now();

    @Default
    @OneToMany(mappedBy = "user")
    private Set<UserRole> userRoles = new HashSet<>();

    public boolean hasRole(String roleName) {
        return userRoles.stream().anyMatch(role -> role.getRole().getName().equalsIgnoreCase(roleName));
    }
}
