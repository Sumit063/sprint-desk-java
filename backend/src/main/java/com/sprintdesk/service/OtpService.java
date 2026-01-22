package com.sprintdesk.service;

import com.sprintdesk.model.OtpCode;
import com.sprintdesk.repository.OtpCodeRepository;
import com.sprintdesk.security.SecurityProperties;
import com.sprintdesk.security.TokenService;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Locale;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class OtpService {
  /**
   * Issues and validates one-time passcodes for email-based login.
   */
  private final OtpCodeRepository otpCodeRepository;
  private final TokenService tokenService;
  private final SecurityProperties securityProperties;
  private final SecureRandom secureRandom = new SecureRandom();

  public OtpService(
      OtpCodeRepository otpCodeRepository,
      TokenService tokenService,
      SecurityProperties securityProperties) {
    this.otpCodeRepository = otpCodeRepository;
    this.tokenService = tokenService;
    this.securityProperties = securityProperties;
  }

  public OtpChallenge requestCode(String email) {
    String normalized = email.toLowerCase(Locale.ROOT).trim();
    otpCodeRepository.deleteByEmail(normalized);

    String code = generateCode(securityProperties.getOtpCodeLength());
    Instant expiresAt = Instant.now().plus(Duration.ofMinutes(securityProperties.getOtpMinutes()));

    OtpCode otp = new OtpCode();
    otp.setEmail(normalized);
    otp.setCodeHash(tokenService.hashToken(code));
    otp.setExpiresAt(expiresAt);
    otpCodeRepository.save(otp);

    return new OtpChallenge(code, expiresAt);
  }

  public void verifyCode(String email, String code) {
    String normalized = email.toLowerCase(Locale.ROOT).trim();
    OtpCode otp =
        otpCodeRepository
            .findTopByEmailOrderByCreatedAtDesc(normalized)
            .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code"));

    if (otp.getConsumedAt() != null || otp.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Code expired");
    }

    String hashed = tokenService.hashToken(code.trim());
    if (!hashed.equals(otp.getCodeHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid code");
    }

    otp.setConsumedAt(Instant.now());
    otpCodeRepository.save(otp);
  }

  private String generateCode(int length) {
    int safeLength = Math.max(4, Math.min(length, 8));
    StringBuilder builder = new StringBuilder(safeLength);
    for (int i = 0; i < safeLength; i++) {
      builder.append(secureRandom.nextInt(10));
    }
    return builder.toString();
  }

  public record OtpChallenge(String code, Instant expiresAt) {}
}
