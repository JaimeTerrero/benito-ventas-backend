package com.benitoventas.dto;

import com.benitoventas.entities.OrderStatus;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String customerId,
        List<Item> items,
        OrderStatus status,
        Instant createdAt,
        BigDecimal total,
        String rejectionReason,
        Instant processedAt
) 
{
    public record Item(String productId, int quantity){
    }
}
