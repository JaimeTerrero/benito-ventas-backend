package com.benitoventas.dto;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record OrderItemRequest(

    @NotBlank 
    String productId,

    @NotNull
    @Min(1)
    Integer quantity) {}
