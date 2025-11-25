package com.tickets.backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.tickets.backend.dto.event.CreateEventRequest;
import com.tickets.backend.model.Event;
import com.tickets.backend.model.Purchase;
import com.tickets.backend.model.Role;
import com.tickets.backend.model.Ticket;
import com.tickets.backend.model.TicketStatus;
import com.tickets.backend.model.User;
import com.tickets.backend.model.UserRole;
import com.tickets.backend.model.Venue;
import com.tickets.backend.repository.PurchaseRepository;
import com.tickets.backend.service.AuditService;
import com.tickets.backend.service.CurrentUserService;
import com.tickets.backend.service.EventService;
import com.tickets.backend.service.PurchaseService;
import com.tickets.backend.service.UserService;
import com.tickets.backend.service.exception.EventNotFoundException;
import com.tickets.backend.service.model.PurchaseResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class EventControllerTest {

    @Mock
    private EventService eventService;

    @Mock
    private PurchaseService purchaseService;

    @Mock
    private PurchaseRepository purchaseRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private CurrentUserService currentUserService;

    @Mock
    private UserService userService;

    @InjectMocks
    private EventController eventController;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private Event event;
    private Venue venue;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(eventController)
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
        objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();

        venue = Venue.builder()
            .id(UUID.randomUUID())
            .name("Hall")
            .location("City")
            .build();
        event = Event.builder()
            .id(UUID.randomUUID())
            .venue(venue)
            .title("Concert")
            .description("Live")
            .startsAt(OffsetDateTime.now().plusDays(5))
            .endsAt(OffsetDateTime.now().plusDays(5).plusHours(2))
            .faceValueCents(3500)
            .build();
    }

    @Test
    void listEventsReturnsUpcomingEvents() throws Exception {
        when(eventService.listUpcoming()).thenReturn(List.of(event));

        mockMvc.perform(get("/api/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].title", is("Concert")));
    }

    @Test
    void listVenueEventsReturnsEventsForVenue() throws Exception {
        when(eventService.listByVenue(venue.getId())).thenReturn(List.of(event));

        mockMvc.perform(get("/api/venues/{venueId}/events", venue.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].venueId", is(venue.getId().toString())));
    }

    @Test
    void getEventReturnsSingleEvent() throws Exception {
        when(eventService.getById(event.getId())).thenReturn(event);

        mockMvc.perform(get("/api/events/{eventId}", event.getId()))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Concert")));
    }

    @Test
    void getEventReturns404WhenMissing() throws Exception {
        when(eventService.getById(event.getId())).thenThrow(new EventNotFoundException(event.getId()));

        mockMvc.perform(get("/api/events/{eventId}", event.getId()))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.code", is("not_found")));
    }

    @Test
    void createEventPersistsEvent() throws Exception {
        User admin = userWithRole("ROLE_ADMIN");
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(eventService.createEvent(eq(venue.getId()), eq("Concert"), eq("Live"),
            any(OffsetDateTime.class), any(OffsetDateTime.class), eq(3500)))
            .thenReturn(event);

        CreateEventRequest request = new CreateEventRequest(
            "Concert",
            "Live",
            OffsetDateTime.now().plusDays(2),
            OffsetDateTime.now().plusDays(2).plusHours(2),
            3500
        );

        mockMvc.perform(post("/api/venues/{venueId}/events", venue.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title", is("Concert")));

        verify(auditService).log(admin.getEmail(), "EVENT_CREATED", "EVENT", event.getId(), event.getTitle());
    }

    @Test
    void generateTicketsReturnsBatchInfo() throws Exception {
        User manager = userWithRole("ROLE_MANAGER");
        when(currentUserService.requireCurrentUser()).thenReturn(manager);
        when(eventService.getById(event.getId())).thenReturn(event);
        when(eventService.generateTickets(event.getId(), 2)).thenReturn(List.of(
            Ticket.builder().id(UUID.randomUUID()).event(event).status(TicketStatus.AVAILABLE).code("A").build(),
            Ticket.builder().id(UUID.randomUUID()).event(event).status(TicketStatus.AVAILABLE).code("B").build()
        ));
        when(userService.getManagedVenues(manager.getId())).thenReturn(List.of(venue));

        mockMvc.perform(post("/api/events/{id}/tickets:generate", event.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":2}"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.generated", is(2)));
    }

    @Test
    void generateTicketsReturnsConflictWhenManagerHasNoAccess() throws Exception {
        User manager = userWithRole("ROLE_MANAGER");
        when(currentUserService.requireCurrentUser()).thenReturn(manager);
        when(eventService.getById(event.getId())).thenReturn(event);
        when(userService.getManagedVenues(manager.getId())).thenReturn(List.of());

        mockMvc.perform(post("/api/events/{id}/tickets:generate", event.getId())
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"quantity\":1}"))
            .andExpect(status().isConflict())
            .andExpect(jsonPath("$.code", is("conflict")));
    }

    @Test
    void purchasersReturnsJsonList() throws Exception {
        User manager = userWithRole("ROLE_MANAGER");
        when(currentUserService.requireCurrentUser()).thenReturn(manager);
        when(eventService.getById(event.getId())).thenReturn(event);
        when(userService.getManagedVenues(manager.getId())).thenReturn(List.of(venue));
        User namelessManager = User.builder()
            .id(manager.getId())
            .email(manager.getEmail())
            .displayName(null)
            .userRoles(manager.getUserRoles())
            .address("10 Manager Way")
            .phoneNumber("555-9090")
            .build();

        when(purchaseRepository.findAllByEventIdWithUser(event.getId())).thenReturn(List.of(
            Purchase.builder()
                .id(UUID.randomUUID())
                .event(event)
                .user(namelessManager)
                .quantity(2)
                .totalAmountCents(7000)
                .idempotencyKey("key")
                .build()
        ));

        mockMvc.perform(get("/api/events/{eventId}/purchasers", event.getId())
                .accept(MediaType.APPLICATION_JSON))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$", hasSize(1)))
            .andExpect(jsonPath("$[0].email", is(manager.getEmail())))
            .andExpect(jsonPath("$[0].address", is("10 Manager Way")))
            .andExpect(jsonPath("$[0].phoneNumber", is("555-9090")));
    }

    @Test
    void purchasersCsvReturnsAttachment() throws Exception {
        User admin = userWithRole("ROLE_ADMIN");
        admin.setDisplayName("Admin, \"Jazz\"");
        admin.setAddress("123 Winner Rd");
        admin.setPhoneNumber("555-0100");
        when(currentUserService.requireCurrentUser()).thenReturn(admin);
        when(eventService.getById(event.getId())).thenReturn(event);
        User nullNameUser = userWithRole("ROLE_MANAGER");
        nullNameUser.setDisplayName(null);
        nullNameUser.setAddress(null);
        nullNameUser.setPhoneNumber(null);

        User plainUser = userWithRole("ROLE_USER");
        plainUser.setDisplayName("Plain");
        plainUser.setAddress("456 Plain St");
        plainUser.setPhoneNumber("555-0200");

        when(purchaseRepository.findAllByEventIdWithUser(event.getId())).thenReturn(List.of(
            Purchase.builder()
                .id(UUID.randomUUID())
                .event(event)
                .user(admin)
                .quantity(1)
                .totalAmountCents(3500)
                .idempotencyKey("key")
                .build(),
            Purchase.builder()
                .id(UUID.randomUUID())
                .event(event)
                .user(nullNameUser)
                .quantity(1)
                .totalAmountCents(3500)
                .idempotencyKey("key-2")
                .build(),
            Purchase.builder()
                .id(UUID.randomUUID())
                .event(event)
                .user(plainUser)
                .quantity(1)
                .totalAmountCents(3500)
                .idempotencyKey("key-3")
                .build()
        ));

        MvcResult result = mockMvc.perform(get("/api/events/{eventId}/purchasers", event.getId())
                .accept("text/csv"))
            .andExpect(status().isOk())
            .andReturn();

        String csv = result.getResponse().getContentAsString();
        assertThat(csv)
            .contains("email,display_name,address,phone_number,quantity,total_amount_cents,purchased_at")
            .contains("role_admin@example.com,\"Admin, \"\"Jazz\"\"\",123 Winner Rd,555-0100,1,3500")
            .contains("role_manager@example.com,,,,1,3500")
            .contains("role_user@example.com,Plain,456 Plain St,555-0200,1,3500");
    }

    @Test
    void purchaseTicketsReturnsSuccessResponse() throws Exception {
        User user = userWithRole("ROLE_USER");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        Purchase purchase = Purchase.builder()
            .id(UUID.randomUUID())
            .event(event)
            .user(user)
            .quantity(2)
            .totalAmountCents(7000)
            .paymentReference("ref")
            .idempotencyKey("key")
            .build();
        List<Ticket> tickets = List.of(
            Ticket.builder().id(UUID.randomUUID()).event(event).status(TicketStatus.SOLD).code("A").build(),
            Ticket.builder().id(UUID.randomUUID()).event(event).status(TicketStatus.SOLD).code("B").build()
        );
        when(purchaseService.purchaseTickets(eq(user), eq(event.getId()), eq(2), eq("token"), eq("key")))
            .thenReturn(new PurchaseResult(purchase, tickets));

        mockMvc.perform(post("/api/events/{eventId}/purchase", event.getId())
                .header("Idempotency-Key", "key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity":2,"paymentToken":"token"}
                    """))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.quantity", is(2)))
            .andExpect(jsonPath("$.ticketCodes", hasSize(2)));
    }

    @Test
    void purchaseTicketsPropagatesIllegalArgument() throws Exception {
        User user = userWithRole("ROLE_USER");
        when(currentUserService.requireCurrentUser()).thenReturn(user);
        when(purchaseService.purchaseTickets(eq(user), eq(event.getId()), eq(1), eq("token"), eq("key")))
            .thenThrow(new IllegalArgumentException("bad request"));

        mockMvc.perform(post("/api/events/{eventId}/purchase", event.getId())
                .header("Idempotency-Key", "key")
                .contentType(MediaType.APPLICATION_JSON)
                .content("""
                    {"quantity":1,"paymentToken":"token"}
                    """))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.code", is("bad_request")));
    }

    private User userWithRole(String roleName) {
        Role role = Role.builder().id(roleName.hashCode() & 0xffffL).name(roleName).build();
        User user = User.builder()
            .id(UUID.randomUUID())
            .email(roleName.toLowerCase() + "@example.com")
            .displayName(roleName)
            .build();
        UserRole userRole = UserRole.builder()
            .id(roleName.hashCode() & 0xffffL)
            .user(user)
            .role(role)
            .build();
        user.setUserRoles(new java.util.HashSet<>(Set.of(userRole)));
        return user;
    }
}
