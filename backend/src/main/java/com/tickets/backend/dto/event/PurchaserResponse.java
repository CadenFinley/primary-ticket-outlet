package com.tickets.backend.dto.event;

public record PurchaserResponse(String email,
                                String displayName,
                                String address,
                                String phoneNumber,
                                int quantity,
                                int totalAmountCents,
                                String purchasedAt) {
}

