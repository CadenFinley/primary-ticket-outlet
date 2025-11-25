package com.tickets.backend.controller;

import com.tickets.backend.dto.user.MeResponse;
import com.tickets.backend.model.User;
import com.tickets.backend.model.UserRole;
import com.tickets.backend.model.Venue;
import com.tickets.backend.service.CurrentUserService;
import com.tickets.backend.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class UserControllerTest {

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @InjectMocks
    private UserController userController;

    private MockMvc mockMvc;
    private User user;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(userController).build();
        user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .displayName("User")
            .address("123 Main St")
            .phoneNumber("555-1111")
            .userRoles(new java.util.HashSet<>(Set.of(UserRole.builder().id(1L).build())))
            .build();
    }

    @Test
    void meReturnsCurrentUserProfile() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(userService.getRoleNames(user)).thenReturn(List.of("ROLE_USER"));
        when(userService.getManagedVenues(user.getId())).thenReturn(List.of(
            Venue.builder().id(UUID.randomUUID()).name("Hall").build()
        ));

        mockMvc.perform(get("/api/me"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("user@example.com")))
            .andExpect(jsonPath("$.address", is("123 Main St")))
            .andExpect(jsonPath("$.roles", containsInAnyOrder("ROLE_USER")));
    }

    @Test
    void updateContactInfoSavesAndReturnsProfile() throws Exception {
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        User updated = User.builder()
            .id(user.getId())
            .email(user.getEmail())
            .displayName(user.getDisplayName())
            .address("456 Oak Ave")
            .phoneNumber("555-2222")
            .userRoles(user.getUserRoles())
            .build();
        when(userService.updateContactInfo(user.getId(), "456 Oak Ave", "555-2222")).thenReturn(updated);
        when(userService.getRoleNames(updated)).thenReturn(List.of("ROLE_USER"));
        when(userService.getManagedVenues(updated.getId())).thenReturn(List.of());

        mockMvc.perform(put("/api/me/contact-info")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"address\":\"456 Oak Ave\",\"phoneNumber\":\"555-2222\"}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.address", is("456 Oak Ave")))
            .andExpect(jsonPath("$.phoneNumber", is("555-2222")));
    }
}

