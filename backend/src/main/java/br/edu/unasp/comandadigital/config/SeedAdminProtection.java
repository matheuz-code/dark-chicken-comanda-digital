package br.edu.unasp.comandadigital.config;

import java.util.Optional;

import br.edu.unasp.comandadigital.entity.Usuario;
import br.edu.unasp.comandadigital.entity.enums.StatusAtivacao;
import br.edu.unasp.comandadigital.repository.UsuarioRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Prevents the known administrator seeded by V2 from being usable in a deployed environment.
 * Set INITIAL_ADMIN_PASSWORD (minimum 12 characters) when configuring a new environment.
 * Never changes the password of an administrator who already has a custom password.
 */
@Component
@ConditionalOnProperty(prefix = "app.security.seed-admin", name = "enabled", havingValue = "true", matchIfMissing = true)
public class SeedAdminProtection implements ApplicationRunner {
    private static final Logger log = LoggerFactory.getLogger(SeedAdminProtection.class);
    private static final String DEFAULT_SEED_EMAIL = "admin@email.com";
    private static final String DEFAULT_SEED_PASSWORD = "senha123";

    private final UsuarioRepository repository;
    private final PasswordEncoder passwordEncoder;
    private final String initialAdminPassword;

    public SeedAdminProtection(
            UsuarioRepository repository,
            PasswordEncoder passwordEncoder,
            @Value("${INITIAL_ADMIN_PASSWORD:}") String initialAdminPassword) {
        this.repository = repository;
        this.passwordEncoder = passwordEncoder;
        this.initialAdminPassword = initialAdminPassword;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Optional<Usuario> seedAdmin = repository.findByEmailIgnoreCase(DEFAULT_SEED_EMAIL);
        if (seedAdmin.isEmpty()) {
            return;
        }

        Usuario admin = seedAdmin.get();
        if (!passwordEncoder.matches(DEFAULT_SEED_PASSWORD, admin.getSenhaHash())) {
            // Administrator already changed their password: don't reset or reactivate it.
            return;
        }

        if (!initialAdminPassword.isBlank()) {
            if (initialAdminPassword.length() < 12) {
                throw new IllegalStateException("INITIAL_ADMIN_PASSWORD deve possuir pelo menos 12 caracteres");
            }
            if (DEFAULT_SEED_PASSWORD.equals(initialAdminPassword)) {
                throw new IllegalStateException("INITIAL_ADMIN_PASSWORD nao pode usar a senha padrao");
            }
            admin.setSenhaHash(passwordEncoder.encode(initialAdminPassword));
            admin.setStatus(StatusAtivacao.ATIVO);
            repository.save(admin);
            log.warn("A credencial administrativa inicial foi substituída. Remova INITIAL_ADMIN_PASSWORD do ambiente após provisionar.");
        } else {
            admin.setStatus(StatusAtivacao.INATIVO);
            repository.save(admin);
            log.warn("Administrador de demonstração desativado: configure INITIAL_ADMIN_PASSWORD para provisionar uma senha segura.");
        }
    }
}
