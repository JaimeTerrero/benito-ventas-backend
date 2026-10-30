package com.benitoventas.messaging.events;

import java.math.BigDecimal;
import java.time.Instant;

public record OrderProcessedEvent(
    String eventId,
    String orderId,
    boolean approved,
    BigDecimal total,
    String rejectionReason,
    Instant processedAt
) {

}
