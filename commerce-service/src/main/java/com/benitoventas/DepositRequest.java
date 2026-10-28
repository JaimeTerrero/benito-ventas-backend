package com.benitoventas;

import java.math.BigDecimal;

import io.smallrye.common.constraint.NotNull;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record DepositRequest(

    @NotBlank 
    @Size(max = 100)
    String reference,

    @NotNull 
    @DecimalMin("0.01")
    @Digits(integer = 12, fraction = 2)
    BigDecimal amount) {}
