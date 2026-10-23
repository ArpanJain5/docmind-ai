package com.docmind.backend.service;

import com.docmind.backend.model.User;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;

@Service
public class TokenService {

    private final JwtEncoder jwtEncoder;
    private final Duration expiry;

    public TokenService(JwtEncoder jwtEncoder,
                        @Value("${app.jwt.expiry-minutes}") long expiryMinutes) {
        this.jwtEncoder = jwtEncoder;
        this.expiry = Duration.ofMinutes(expiryMinutes);
    }

    public String createToken(User user) {
        Instant now = Instant.now();
        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer("docmind")
                .issuedAt(now)
                .expiresAt(now.plus(expiry))
                .subject(String.valueOf(user.getId()))   // who this token is about
                .build();

        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).build();
        return jwtEncoder.encode(JwtEncoderParameters.from(header, claims)).getTokenValue();
    }

    public long expirySeconds() {
        return expiry.toSeconds();
    }
}