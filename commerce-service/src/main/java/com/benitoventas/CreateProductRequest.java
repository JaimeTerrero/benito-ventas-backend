package com.benitoventas;

import java.math.BigDecimal;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateProductRequest(

    @NotBlank 
    @Size(max = 120)
    String name,

    @NotNull 
    @DecimalMin("0.01")
    BigDecimal price,

    @NotNull 
    @Min(0)
    Integer stock) {}