package br.edu.unasp.comandadigital.support;

import java.net.URI;
import java.util.Set;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;

/** Test-only safety boundary: integration tests must never connect to a hosted database. */
public final class IsolatedDatabaseInitializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
    @Override
    public void initialize(ConfigurableApplicationContext context) {
        String value = context.getEnvironment().getProperty("spring.datasource.url", "");
        try {
            if (!value.startsWith("jdbc:mysql://")) throw new IllegalArgumentException();
            URI uri = URI.create(value.substring("jdbc:".length()));
            if (!Set.of("localhost", "127.0.0.1", "[::1]", "::1").contains(uri.getHost())
                    || !"/dark_chicken_sprint1_test".equals(uri.getPath())
                    || uri.getUserInfo() != null) throw new IllegalArgumentException();
        } catch (IllegalArgumentException | NullPointerException ignored) {
            // Never include the provided URL; it might contain production connection information.
            throw new IllegalStateException("Testes exigem MySQL local e o schema exclusivo dark_chicken_sprint1_test. Consulte docs/SPRINT1.md.");
        }
    }
}
