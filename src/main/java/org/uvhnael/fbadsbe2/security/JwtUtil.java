package org.uvhnael.fbadsbe2.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.util.Date;

@Component
@Slf4j
public class JwtUtil {

    @Value("${jwt.secret:}")
    private String secret;

    @Value("${jwt.expiration-ms:3600000}")
    private long expirationMs;

    private Key key;

    @PostConstruct
    public void init() {
        // If no secret configured or secret too short for HS256, auto-generate a secure key
        try {
            if (secret == null || secret.trim().isEmpty() || secret.getBytes(StandardCharsets.UTF_8).length < 32) {
                log.warn("JWT secret is not provided or too short. Generating a secure random key for JWTs. For production, set 'jwt.secret' to a 32+ byte secret.");
                key = Keys.secretKeyFor(SignatureAlgorithm.HS256);
            } else {
                key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
            }
        } catch (Exception ex) {
            log.error("Failed to initialize JWT signing key: {}", ex.getMessage(), ex);
            throw ex;
        }
    }

    public String generateToken(String username) {
        Date now = new Date();
        Date exp = new Date(now.getTime() + expirationMs);

        return Jwts.builder()
                .setSubject(username)
                .setIssuedAt(now)
                .setExpiration(exp)
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token) {
        try {
            Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token);
            return true;
        } catch (Exception ex) {
            return false;
        }
    }

    public String extractUsername(String token) {
        Claims claims = Jwts.parserBuilder().setSigningKey(key).build().parseClaimsJws(token).getBody();
        return claims.getSubject();
    }
}
