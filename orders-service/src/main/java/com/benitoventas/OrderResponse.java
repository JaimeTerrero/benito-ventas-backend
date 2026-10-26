package com.benitoventas;

import java.time.Instant;
import java.util.List;

public record OrderResponse(
        String id,
        String customerId,
        List<Item> items,
        OrderStatus status,
        Instant createdAt
) 
{
    public record Item(String productId, int quantity){
    }
}
