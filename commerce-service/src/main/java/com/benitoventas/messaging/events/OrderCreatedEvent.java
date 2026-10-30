package com.benitoventas.messaging.events;

import java.time.Instant;
import java.util.List;

public record OrderCreatedEvent(
    String eventId,
    String orderId,
    String customerId,
    List<Item> items,
    Instant ocurredAt) 
    {
    public record Item(String productId, int quantity){}
}
