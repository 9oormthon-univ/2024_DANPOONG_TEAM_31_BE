package com.danpoong.withu.config.auth.jwt;

import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Date;

@Slf4j
@Component
public class JwtUtil {

    @Value("${spring.jwt.secret}")
    private String secretKey;

    private Key getSigningKey() {
        return Keys.hmacShaKeyFor(secretKey.getBytes(StandardCharsets.UTF_8));
    }

    public String createAccessToken(String email, String role, Long familyId) {
        Date expiration = Date.from(Instant.now().plus(1, ChronoUnit.HOURS)); // 1시간 유효
        return Jwts.builder()
                .setId(email)
                .setSubject(role)
                .claim("familyId", familyId) // 가족 ID 포함
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public String createRefreshToken(String email, Long familyId) {
        Date expiration = Date.from(Instant.now().plus(7, ChronoUnit.DAYS)); // 7일 유효
        return Jwts.builder()
                .setId(email)
                .claim("familyId", familyId) // 가족 ID 포함
                .setIssuedAt(new Date())
                .setExpiration(expiration)
                .signWith(getSigningKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public boolean validateToken(String token, String email) {
        try {
            Claims claims = Jwts.parserBuilder()
                    .setSigningKey(getSigningKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            String extractedEmail = claims.getId();
            if (!extractedEmail.equals(email)) {
                log.warn("JWT Token validation failed: Email mismatch");
                return false;
            }

            if (claims.getExpiration().before(new Date())) {
                log.warn("JWT Token validation failed: Token expired");
                return false;
            }

            return true;
        } catch (JwtException e) {
            log.warn("JWT validation failed: {}", e.getMessage());
            return false;
        }
    }

    public String extractEmail(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .getId();
    }

    public Long extractFamilyId(String token) {
        return Jwts.parserBuilder()
                .setSigningKey(getSigningKey())
                .build()
                .parseClaimsJws(token)
                .getBody()
                .get("familyId", Long.class); // 가족 ID 추출
    }
}
