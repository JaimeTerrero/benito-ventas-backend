package com.benitoventas;

import jakarta.annotation.Priority;
import jakarta.inject.Inject;
import jakarta.ws.rs.Priorities;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.ext.Provider;

@Provider
@Priority(Priorities.AUTHORIZATION)
public class AdminCertificateFilter implements ContainerRequestFilter {

    @Inject
    AdminCertificateGuard certificateGuard;

    @Override
    public void filter(ContainerRequestContext context) {
        if (context.getSecurityContext().isUserInRole("ADMIN")) {
            certificateGuard.requireAdminCertificate();
        }
    }
}