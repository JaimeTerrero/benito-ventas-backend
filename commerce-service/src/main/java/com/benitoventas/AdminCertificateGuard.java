package com.benitoventas;

import java.security.cert.X509Certificate;

import javax.net.ssl.SSLPeerUnverifiedException;
import javax.security.auth.x500.X500Principal;

import io.vertx.ext.web.RoutingContext;
import jakarta.enterprise.context.RequestScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.ForbiddenException;

@RequestScoped
public class AdminCertificateGuard {

    private static final X500Principal ADMIN_SUBJECT = new X500Principal("CN=admin,O=Benito Ventas");

    @Inject
    RoutingContext routingContext;

    public void requireAdminCertificate() {
        var session = routingContext.request().sslSession();

        if (session == null) {
            throw new ForbiddenException("El acceso administrativo requiere HTTPS y certificado");
        }

        try {
            var certificates = session.getPeerCertificates();

            if (certificates.length == 0 || !(certificates[0] instanceof X509Certificate certificate) || !ADMIN_SUBJECT.equals(certificate.getSubjectX500Principal())) {

                throw new ForbiddenException("Se requiere el certificado administrativo");
            }
        } catch (SSLPeerUnverifiedException exception) {
            throw new ForbiddenException("Se requiere el certificado administrativo");
        }
    }
}