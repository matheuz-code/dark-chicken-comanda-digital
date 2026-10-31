package br.edu.unasp.comandadigital.controller;

import java.time.OffsetDateTime;
import java.sql.Connection;
import javax.sql.DataSource;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class SystemController {
    private final DataSource dataSource;

    public SystemController(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @GetMapping("/ready")
    public ResponseEntity<SystemStatusResponse> ready() {
        try (Connection connection = dataSource.getConnection()) {
            if (connection.isValid(2)) {
                return ResponseEntity.ok(new SystemStatusResponse("Comanda Digital API", "READY", OffsetDateTime.now()));
            }
        } catch (Exception ignored) {
            // Não expor detalhes de JDBC, DNS ou credenciais no endpoint público.
        }
        return ResponseEntity.status(503).body(new SystemStatusResponse("Comanda Digital API", "UNAVAILABLE", OffsetDateTime.now()));
    }

    @GetMapping("/status")
    public ResponseEntity<SystemStatusResponse> status() {
        return ResponseEntity.ok(new SystemStatusResponse(
                "Comanda Digital API",
                "UP",
                OffsetDateTime.now()));
    }

    public record SystemStatusResponse(String application, String status, OffsetDateTime timestamp) {
    }
}
