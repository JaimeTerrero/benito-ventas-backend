package com.benitoventas;

public record LoginResponse(
    String accessToken,
    String tokenType,
    long expiresIn
) {

}
