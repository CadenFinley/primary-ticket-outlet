package com.tickets.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.backend.dto.auth.MockLoginRequest;
import com.tickets.backend.dto.auth.TokenPayload;
import com.tickets.backend.model.User;
import com.tickets.backend.service.AuthTokenService;
import com.tickets.backend.service.UserService;
import com.tickets.backend.service.VenueService;
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
import java.util.UUID;

import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class AuthControllerTest {

    @Mock
    private UserService userService;

    @Mock
    private VenueService venueService;

    @Mock
    private AuthTokenService authTokenService;

    @InjectMocks
    private AuthController authController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(authController).build();
        objectMapper = new ObjectMapper();
    }

    @Test
    void mockLoginReturnsAuthResponse() throws Exception {
        User user = User.builder()
            .id(UUID.randomUUID())
            .email("user@example.com")
            .displayName("User")
            .address("123 Main St")
            .phoneNumber("555-1111")
            .build();
        when(userService.findOrCreateUser("user@example.com", "User")).thenReturn(user);
        when(userService.getRoleNames(user)).thenReturn(List.of("ROLE_USER"));
        when(authTokenService.generateToken(any(TokenPayload.class))).thenReturn("token");

        MockLoginRequest request = new MockLoginRequest("user@example.com", "User", List.of("ROLE_USER"), List.of());

        mockMvc.perform(post("/api/auth/mock")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.email", is("user@example.com")))
            .andExpect(jsonPath("$.address", is("123 Main St")))
            .andExpect(jsonPath("$.token", is("token")));

        verify(userService).ensureRoles(user, List.of("ROLE_USER"));
    }
}

