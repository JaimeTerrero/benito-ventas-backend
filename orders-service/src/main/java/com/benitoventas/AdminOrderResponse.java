package com.benitoventas;

import java.time.Instant;

public record AdminOrderResponse(
    OrderResponse order,
    String adminNote,
    String updatedBy,
    Instant updatedAt) {}