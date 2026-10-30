package com.benitoventas.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record UpdateOrderNoteRequest(

    @NotNull
    @Size(max = 1000)
    String note) {}