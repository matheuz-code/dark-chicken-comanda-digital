package br.edu.unasp.comandadigital.service;

import java.time.Instant;

import br.edu.unasp.comandadigital.config.JwtProperties;
import br.edu.unasp.comandadigital.entity.Usuario;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

@Service
public class JwtService {

    private final JwtEncoder jwtEncoder;
    private final JwtProperties properties;

    public JwtService(JwtEncoder jwtEncoder, JwtProperties properties) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
    }

    public TokenGerado gerarToken(Usuario usuario) {
        Instant emitidoEm = Instant.now();
        Instant expiraEm = emitidoEm.plus(properties.expiration());

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .issuedAt(emitidoEm)
                .expiresAt(expiraEm)
                .subject(usuario.getEmail())
                .claim("role", usuario.getPerfil().name())
                .build();

        String token = jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
        return new TokenGerado(token, properties.expiration().toSeconds());
    }

    public record TokenGerado(String valor, long expiraEmSegundos) {
    }
}
