package com.sprintdesk.security;

import com.sprintdesk.model.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.security.Key;
import java.time.Duration;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class JwtService {
  private final SecurityProperties properties;
  private final Key signingKey;

  public JwtService(SecurityProperties properties) {
    this.properties = properties;
    this.signingKey = buildKey(properties.getJwtSecret());
  }

  public String generateAccessToken(User user) {
    Instant now = Instant.now();
    Instant expiresAt = now.plus(Duration.ofMinutes(properties.getAccessTokenMinutes()));
    return Jwts.builder()
        .setSubject(user.getId().toString())
        .claim("role", user.getRole().name())
        .setIssuedAt(Date.from(now))
        .setExpiration(Date.from(expiresAt))
        .signWith(signingKey, SignatureAlgorithm.HS256)
        .compact();
  }

  public Claims parseAccessToken(String token) {
    return Jwts.parserBuilder()
        .setSigningKey(signingKey)
        .build()
        .parseClaimsJws(token)
        .getBody();
  }

  private Key buildKey(String secret) {
    byte[] keyBytes =
        secret == null ? new byte[32] : secret.getBytes(StandardCharsets.UTF_8);
    if (keyBytes.length < 32) {
      keyBytes = Arrays.copyOf(keyBytes, 32);
    }
    return Keys.hmacShaKeyFor(keyBytes);
  }

  public UUID parseUserId(Claims claims) {
    return UUID.fromString(claims.getSubject());
  }
}
