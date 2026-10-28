package com.benitoventas;

import java.math.BigDecimal;
import java.time.Instant;

public record CustomerResponse(
    String id,
    String name,
    String email,
    BigDecimal balance,
    Instant createdAt
) {}
