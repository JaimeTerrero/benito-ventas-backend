package com.benitoventas.bootstrap;

import com.benitoventas.entities.Customer;
import com.benitoventas.repositories.CustomerRepository;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Locale;
import java.util.Optional;

import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;

import io.quarkus.elytron.security.common.BcryptUtil;
import io.quarkus.runtime.StartupEvent;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.event.Observes;
import jakarta.inject.Inject;


@ApplicationScoped
public class AdminBootstrap {

    private static final Logger LOG = Logger.getLogger(AdminBootstrap.class);

    @ConfigProperty(name = "app.bootstrap.admin.enabled", defaultValue = "false")
    boolean enabled;

    @ConfigProperty(name = "app.bootstrap.admin.email")
    Optional<String> configuredEmail;

    @ConfigProperty(name = "app.bootstrap.admin.password")
    Optional<String> configuredPassword;

    private final CustomerRepository customerRepository;

    @Inject
    public AdminBootstrap(CustomerRepository customerRepository) {
        this.customerRepository = customerRepository;
    }

    void onStart(@Observes StartupEvent event) {
        if (!enabled) {
            return;
        }

        String email = configuredEmail.orElseThrow(() -> new IllegalStateException("Falta el correo del administrador"))
                                        .strip()
                                        .toLowerCase(Locale.ROOT);

        if (email.isBlank()) {
            throw new IllegalStateException("El correo del administrador esta vacio");
        }

        Customer existing = customerRepository.findByEmail(email);

        if (existing != null) {
            if (!"ADMIN".equals(existing.role)) {
                throw new IllegalStateException("El correo pertenece a un cliente; no se cambiara su rol");
            }

            LOG.info("La cuenta administradora ya existe");
            return;
        }

        String password = configuredPassword.orElseThrow(() -> new IllegalStateException("Falta la contraseña del administrador"));

        if (password.length() < 12 || password.getBytes(StandardCharsets.UTF_8).length > 72) {
            throw new IllegalStateException("La contraseña requiere al menos 12 y hasta 72 caracteres");
        }

        var admin = new Customer();
        admin.name = "Administrador";
        admin.email = email;
        admin.passwordHash = BcryptUtil.bcryptHash(password);
        admin.role = "ADMIN";
        admin.balance = new BigDecimal("0.00");
        admin.createdAt = Instant.now();

        customerRepository.persist(admin);

        LOG.info("Cuenta administradora creada");
    }
}
