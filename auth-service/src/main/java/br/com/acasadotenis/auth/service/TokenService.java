package br.com.acasadotenis.auth.service;

import br.com.acasadotenis.auth.domain.StaffMember;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.*;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

/** Emite o JWT da equipe. Os outros serviços validam com o mesmo segredo (JWT_SECRET). */
@Service
public class TokenService {

    private final JwtEncoder encoder;
    private final Duration ttl;

    public TokenService(JwtEncoder encoder, @Value("${nexum.jwt.ttl-hours:10}") long ttlHours) {
        this.encoder = encoder;
        this.ttl = Duration.ofHours(ttlHours);
    }

    public record Token(String value, Instant expiresAt) {}

    public Token issue(StaffMember member) {
        Instant now = Instant.now();
        Instant exp = now.plus(ttl);
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("nexum-auth")
                .subject(member.getEmail())
                .issuedAt(now)
                .expiresAt(exp)
                .claim("name", member.getName())
                .claim("role", member.getRole().name())
                .claim("uid", member.getId())
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return new Token(encoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue(), exp);
    }
}
