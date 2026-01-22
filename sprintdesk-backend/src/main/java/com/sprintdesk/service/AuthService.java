package com.sprintdesk.service;

import com.sprintdesk.dto.AuthResponse;
import com.sprintdesk.dto.LoginRequest;
import com.sprintdesk.dto.RegisterRequest;
import com.sprintdesk.dto.UserResponse;
import com.sprintdesk.model.RefreshToken;
import com.sprintdesk.model.Role;
import com.sprintdesk.model.User;
import com.sprintdesk.repository.RefreshTokenRepository;
import com.sprintdesk.repository.UserRepository;
import com.sprintdesk.security.JwtService;
import com.sprintdesk.security.TokenService;
import java.time.Instant;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {
  private final UserRepository userRepository;
  private final RefreshTokenRepository refreshTokenRepository;
  private final PasswordEncoder passwordEncoder;
  private final JwtService jwtService;
  private final TokenService tokenService;

  public AuthService(
      UserRepository userRepository,
      RefreshTokenRepository refreshTokenRepository,
      PasswordEncoder passwordEncoder,
      JwtService jwtService,
      TokenService tokenService) {
    this.userRepository = userRepository;
    this.refreshTokenRepository = refreshTokenRepository;
    this.passwordEncoder = passwordEncoder;
    this.jwtService = jwtService;
    this.tokenService = tokenService;
  }

  public AuthPayload register(RegisterRequest request) {
    String email = request.email().toLowerCase();
    if (userRepository.existsByEmailIgnoreCase(email)) {
      throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already in use");
    }

    User user = new User();
    user.setEmail(email);
    user.setName(request.name().trim());
    user.setPasswordHash(passwordEncoder.encode(request.password()));
    user.setRole(Role.MEMBER);
    userRepository.save(user);

    return issueTokens(user);
  }

  public AuthPayload login(LoginRequest request) {
    String email = request.email().toLowerCase();
    User user =
        userRepository
            .findByEmailIgnoreCase(email)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials"));

    if (!passwordEncoder.matches(request.password(), user.getPasswordHash())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid credentials");
    }

    return issueTokens(user);
  }

  public AuthPayload refresh(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing refresh token");
    }

    String tokenHash = tokenService.hashToken(refreshToken);
    RefreshToken stored =
        refreshTokenRepository
            .findByTokenHash(tokenHash)
            .orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalid"));

    if (stored.getRevokedAt() != null || stored.getExpiresAt().isBefore(Instant.now())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Refresh token invalid");
    }

    stored.setRevokedAt(Instant.now());
    refreshTokenRepository.save(stored);

    return issueTokens(stored.getUser());
  }

  public void logout(String refreshToken) {
    if (refreshToken == null || refreshToken.isBlank()) {
      return;
    }
    String tokenHash = tokenService.hashToken(refreshToken);
    refreshTokenRepository
        .findByTokenHash(tokenHash)
        .ifPresent(
            stored -> {
              if (stored.getRevokedAt() == null) {
                stored.setRevokedAt(Instant.now());
                refreshTokenRepository.save(stored);
              }
            });
  }

  private AuthPayload issueTokens(User user) {
    String accessToken = jwtService.generateAccessToken(user);
    String refreshToken = tokenService.generateRefreshToken();

    RefreshToken stored = new RefreshToken();
    stored.setUser(user);
    stored.setTokenHash(tokenService.hashToken(refreshToken));
    stored.setExpiresAt(tokenService.refreshTokenExpiry());
    refreshTokenRepository.save(stored);

    return new AuthPayload(new AuthResponse(accessToken, toUserResponse(user)), refreshToken);
  }

  private UserResponse toUserResponse(User user) {
    return new UserResponse(
        user.getId().toString(),
        user.getEmail(),
        user.getName(),
        user.getAvatarUrl(),
        user.getContact());
  }
}