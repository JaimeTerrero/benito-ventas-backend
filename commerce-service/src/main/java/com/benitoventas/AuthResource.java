package com.benitoventas;

import jakarta.annotation.security.PermitAll;
import jakarta.inject.Inject;
import jakarta.validation.Valid;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.validation.constraints.NotNull;

@Path("/auth")
@Consumes(MediaType.APPLICATION_JSON)
@Produces(MediaType.APPLICATION_JSON)
public class AuthResource {

    private final AuthService authService;

    @Inject 
    public AuthResource(AuthService authService){
        this.authService = authService;
    }

    @POST
    @Path("/login")
    @PermitAll
    public LoginResponse login(@NotNull @Valid LoginRequest request) {
        return authService.login(request);
    }
}
