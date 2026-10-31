package br.edu.unasp.comandadigital.support;

import org.junit.jupiter.api.Test;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.mock.env.MockEnvironment;
import static org.assertj.core.api.Assertions.*;

class IsolatedDatabaseInitializerTest {
    private void initialize(String url) {
        try (var context = new GenericApplicationContext()) {
            context.setEnvironment(new MockEnvironment().withProperty("spring.datasource.url", url));
            new IsolatedDatabaseInitializer().initialize(context);
        }
    }
    @Test void acceptsOnlyTheDedicatedLocalTestDatabase() {
        assertThatCode(() -> initialize("jdbc:mysql://127.0.0.1:33316/dark_chicken_sprint1_test?sslMode=DISABLED")).doesNotThrowAnyException();
    }
    @Test void refusesHostedDatabaseWithoutPrintingTheUrl() {
        assertThatThrownBy(() -> initialize("jdbc:mysql://private.example.invalid:3306/dark_chicken_sprint1_test"))
                .isInstanceOf(IllegalStateException.class).hasMessageNotContaining("private.example.invalid");
    }
    @Test void refusesTheApplicationDatabaseEvenOnLocalhost() {
        assertThatThrownBy(() -> initialize("jdbc:mysql://localhost:3306/comanda_digital")).isInstanceOf(IllegalStateException.class);
    }
    @Test void refusesAnotherDatabaseEngine() {
        assertThatThrownBy(() -> initialize("jdbc:h2:mem:test")).isInstanceOf(IllegalStateException.class);
    }
}
