package com.benitoventas.services;

import com.benitoventas.dto.LoginRequest;
import com.benitoventas.dto.LoginResponse;
import com.benitoventas.repositories.CustomerRepository;
import com.benitoventas.security.AdminCertificateGuard;

import java.nio.charset.StandardCharsets;
import java.util.Locale;
import java.util.Set;

import org.eclipse.microprofile.config.inject.ConfigProperty;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.smallrye.jwt.build.Jwt;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.WebApplicationException;

@ApplicationScoped
public class AuthService {

    private final CustomerRepository customerRepository;
    @ConfigProperty(name = "smallrye.jwt.new-token.lifespan")
    long tokenLifespan;
    private final AdminCertificateGuard certificateGuard;

    @Inject
    public AuthService(CustomerRepository customerRepository, AdminCertificateGuard certificateGuard){
        this.customerRepository = customerRepository;
        this.certificateGuard = certificateGuard;
    }

    public LoginResponse login(LoginRequest request){
        String email = request.email().strip().toLowerCase(Locale.ROOT);

        if (request.password().getBytes(StandardCharsets.UTF_8).length > 72) {
            throw invalidCredentials();
        }

        var customer = customerRepository.findByEmail(email);

        if (customer == null || customer.passwordHash == null || !BcryptUtil.matches(request.password(), customer.passwordHash)) 
        {
            throw invalidCredentials();
        }

        if (!"CLIENTE".equals(customer.role) && !"ADMIN".equals(customer.role)) 
        {
            throw invalidCredentials();
        }

        if ("ADMIN".equals(customer.role)) {
            certificateGuard.requireAdminCertificate();
        }

        String token = Jwt.issuer("https://benito-ventas.local")
                .subject(customer.id.toHexString())
                .upn(customer.email)
                .audience("benito-ventas-api")
                .groups(Set.of(customer.role))
                .expiresIn(tokenLifespan)
                .sign();

        return new LoginResponse(
                token,
                "Bearer",
                tokenLifespan
        );
    }

    private WebApplicationException invalidCredentials() {
        return new WebApplicationException("Correo o contraseña incorrectos", 401);
    }
}
