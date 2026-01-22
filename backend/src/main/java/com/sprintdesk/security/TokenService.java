package com.sprintdesk.security;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import org.springframework.stereotype.Service;

@Service
public class TokenService {
  private final SecurityProperties properties;
  private final SecureRandom secureRandom = new SecureRandom();

  public TokenService(SecurityProperties properties) {
    this.properties = properties;
  }

  public String generateRefreshToken() {
    byte[] bytes = new byte[64];
    secureRandom.nextBytes(bytes);
    return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
  }

  public String hashToken(String token) {
    try {
      MessageDigest digest = MessageDigest.getInstance("SHA-256");
      byte[] hashed = digest.digest(token.getBytes(StandardCharsets.UTF_8));
      StringBuilder builder = new StringBuilder(hashed.length * 2);
      for (byte value : hashed) {
        builder.append(String.format("%02x", value));
      }
      return builder.toString();
    } catch (NoSuchAlgorithmException ex) {
      throw new IllegalStateException("Unable to hash token", ex);
    }
  }

  public Instant refreshTokenExpiry() {
    return Instant.now().plus(Duration.ofDays(properties.getRefreshTokenDays()));
  }

  public Duration refreshTokenDuration() {
    return Duration.ofDays(properties.getRefreshTokenDays());
  }
}