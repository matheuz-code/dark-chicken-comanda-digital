package br.edu.unasp.comandadigital.controller;

import java.time.OffsetDateTime;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class SystemController {

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
