package com.tickets.backend.controller;

import com.tickets.backend.dto.event.CreateEventRequest;
import com.tickets.backend.dto.event.EventResponse;
import com.tickets.backend.dto.event.GenerateTicketsRequest;
import com.tickets.backend.dto.event.PurchaseRequest;
import com.tickets.backend.dto.event.PurchaseResponse;
import com.tickets.backend.dto.event.PurchaserResponse;
import com.tickets.backend.dto.event.TicketBatchResponse;
import com.tickets.backend.model.Event;
import com.tickets.backend.model.User;
import com.tickets.backend.model.Venue;
import com.tickets.backend.repository.PurchaseRepository;
import com.tickets.backend.service.AuditService;
import com.tickets.backend.service.CurrentUserService;
import com.tickets.backend.service.EventService;
import com.tickets.backend.service.PurchaseService;
import com.tickets.backend.service.UserService;
import com.tickets.backend.service.model.PurchaseResult;
import jakarta.validation.Valid;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api")
public class EventController {

    private final EventService eventService;
    private final PurchaseService purchaseService;
    private final PurchaseRepository purchaseRepository;
    private final AuditService auditService;
    private final CurrentUserService currentUserService;
    private final UserService userService;

    public EventController(EventService eventService,
                           PurchaseService purchaseService,
                           PurchaseRepository purchaseRepository,
                           AuditService auditService,
                           CurrentUserService currentUserService,
                           UserService userService) {
        this.eventService = eventService;
        this.purchaseService = purchaseService;
        this.purchaseRepository = purchaseRepository;
        this.auditService = auditService;
        this.currentUserService = currentUserService;
        this.userService = userService;
    }

    @GetMapping("/events")
    public ResponseEntity<List<EventResponse>> listEvents() {
        List<EventResponse> events = eventService.listUpcoming().stream()
            .map(EventResponse::fromModel)
            .toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/venues/{venueId}/events")
    public ResponseEntity<List<EventResponse>> listVenueEvents(@PathVariable UUID venueId) {
        List<EventResponse> events = eventService.listByVenue(venueId).stream()
            .map(EventResponse::fromModel)
            .toList();
        return ResponseEntity.ok(events);
    }

    @GetMapping("/events/{eventId}")
    public ResponseEntity<EventResponse> getEvent(@PathVariable UUID eventId) {
        Event event = eventService.getById(eventId);
        return ResponseEntity.ok(EventResponse.fromModel(event));
    }

    @PostMapping("/venues/{venueId}/events")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<EventResponse> createEvent(@PathVariable UUID venueId,
                                                     @Valid @RequestBody CreateEventRequest request) {
        User actor = currentUserService.requireCurrentUser();
        ensureManagerAccess(actor, venueId);
        Event event = eventService.createEvent(
            venueId,
            request.title(),
            request.description(),
            request.startsAt(),
            request.endsAt(),
            request.faceValueCents()
        );
        auditService.log(actor.getEmail(), "EVENT_CREATED", "EVENT", event.getId(), event.getTitle());
        return ResponseEntity.ok(EventResponse.fromModel(event));
    }

    @PostMapping("/events/{eventId}/tickets:generate")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<TicketBatchResponse> generateTickets(@PathVariable UUID eventId,
                                                               @Valid @RequestBody GenerateTicketsRequest request) {
        User actor = currentUserService.requireCurrentUser();
        ensureManagerAccess(actor, eventService.getById(eventId).getVenue().getId());
        var tickets = eventService.generateTickets(eventId, request.quantity());
        auditService.log(actor.getEmail(), "TICKETS_GENERATED", "EVENT", eventId, "quantity=" + request.quantity());
        return ResponseEntity.ok(new TicketBatchResponse(tickets.size()));
    }

    @GetMapping(value = "/events/{eventId}/purchasers", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<List<PurchaserResponse>> getPurchasers(@PathVariable UUID eventId) {
        User actor = currentUserService.requireCurrentUser();
        ensureManagerAccess(actor, eventService.getById(eventId).getVenue().getId());
        var purchases = purchaseRepository.findAllByEventIdWithUser(eventId).stream()
            .map(purchase -> new PurchaserResponse(
                purchase.getUser().getEmail(),
                purchase.getUser().getDisplayName(),
                purchase.getUser().getAddress(),
                purchase.getUser().getPhoneNumber(),
                purchase.getQuantity(),
                purchase.getTotalAmountCents(),
                purchase.getCreatedAt().toString()
            ))
            .toList();
        return ResponseEntity.ok(purchases);
    }

    @GetMapping(value = "/events/{eventId}/purchasers", produces = "text/csv")
    @PreAuthorize("hasAnyRole('MANAGER','ADMIN')")
    public ResponseEntity<String> getPurchasersCsv(@PathVariable UUID eventId) {
        User actor = currentUserService.requireCurrentUser();
        ensureManagerAccess(actor, eventService.getById(eventId).getVenue().getId());
        StringBuilder csv = new StringBuilder("email,display_name,address,phone_number,quantity,total_amount_cents,purchased_at\n");
        purchaseRepository.findAllByEventIdWithUser(eventId).forEach(purchase -> csv
            .append(purchase.getUser().getEmail()).append(',')
            .append(escapeCsv(purchase.getUser().getDisplayName())).append(',')
            .append(escapeCsv(purchase.getUser().getAddress())).append(',')
            .append(escapeCsv(purchase.getUser().getPhoneNumber())).append(',')
            .append(purchase.getQuantity()).append(',')
            .append(purchase.getTotalAmountCents()).append(',')
            .append(purchase.getCreatedAt()).append('\n'));
        return ResponseEntity.ok()
            .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=purchasers.csv")
            .body(csv.toString());
    }

    @PostMapping("/events/{eventId}/purchase")
    @PreAuthorize("hasRole('USER')")
    public ResponseEntity<PurchaseResponse> purchase(@PathVariable UUID eventId,
                                                     @Valid @RequestBody PurchaseRequest request,
                                                     @RequestHeader(name = "Idempotency-Key") String idempotencyKey) {
        User user = currentUserService.requireCurrentUser();
        PurchaseResult result = purchaseService.purchaseTickets(
            user,
            eventId,
            request.quantity(),
            request.paymentToken(),
            idempotencyKey
        );
        return ResponseEntity.ok(PurchaseResponse.from(result));
    }

    private void ensureManagerAccess(User actor, UUID venueId) {
        boolean isAdmin = actor.hasRole("ROLE_ADMIN");
        if (isAdmin) {
            return;
        }
        boolean managesVenue = userService.getManagedVenues(actor.getId()).stream()
            .map(Venue::getId)
            .anyMatch(venueId::equals);
        if (!managesVenue) {
            throw new IllegalStateException("Manager does not have access to this venue");
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
