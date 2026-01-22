package com.sprintdesk.controller;

import com.sprintdesk.dto.AuthResponse;
import com.sprintdesk.dto.DemoLoginRequest;
import com.sprintdesk.dto.GoogleLoginRequest;
import com.sprintdesk.dto.LoginRequest;
import com.sprintdesk.dto.OtpRequest;
import com.sprintdesk.dto.OtpVerifyRequest;
import com.sprintdesk.dto.RegisterRequest;
import com.sprintdesk.security.RefreshCookieService;
import com.sprintdesk.security.SecurityProperties;
import com.sprintdesk.service.AuthPayload;
import com.sprintdesk.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Duration;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.WebUtils;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
  private final AuthService authService;
  private final RefreshCookieService refreshCookieService;
  private final SecurityProperties properties;

  public AuthController(
      AuthService authService,
      RefreshCookieService refreshCookieService,
      SecurityProperties properties) {
    this.authService = authService;
    this.refreshCookieService = refreshCookieService;
    this.properties = properties;
  }

  @PostMapping("/register")
  public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
    AuthPayload payload = authService.register(request);
    return buildResponse(payload);
  }

  @PostMapping("/login")
  public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
    AuthPayload payload = authService.login(request);
    return buildResponse(payload);
  }

  @PostMapping("/refresh")
  public ResponseEntity<AuthResponse> refresh(HttpServletRequest request) {
    String token = extractRefreshToken(request);
    AuthPayload payload = authService.refresh(token);
    return buildResponse(payload);
  }

  @PostMapping("/logout")
  public ResponseEntity<Map<String, Object>> logout(HttpServletRequest request) {
    String token = extractRefreshToken(request);
    authService.logout(token);

    HttpHeaders headers = new HttpHeaders();
    refreshCookieService.clearCookie(headers);

    return ResponseEntity.ok().headers(headers).body(Map.of("ok", true));
  }

  @PostMapping("/demo")
  public ResponseEntity<AuthResponse> demoLogin(@Valid @RequestBody DemoLoginRequest request) {
    AuthPayload payload = authService.loginDemo(request.type());
    return buildResponse(payload);
  }

  @PostMapping("/google")
  public ResponseEntity<AuthResponse> googleLogin(@Valid @RequestBody GoogleLoginRequest request) {
    AuthPayload payload = authService.loginWithGoogle(request.credential());
    return buildResponse(payload);
  }

  @PostMapping("/otp/request")
  public ResponseEntity<Map<String, Object>> otpRequest(@Valid @RequestBody OtpRequest request) {
    var challenge = authService.requestOtp(request.email());
    Map<String, Object> response = new HashMap<>();
    response.put("ok", true);
    response.put("expiresAt", challenge.expiresAt().toString());
    if (properties.isOtpReturnCode()) {
      response.put("code", challenge.code());
    }
    return ResponseEntity.ok(response);
  }

  @PostMapping("/otp/verify")
  public ResponseEntity<AuthResponse> otpVerify(@Valid @RequestBody OtpVerifyRequest request) {
    AuthPayload payload = authService.loginWithOtp(request.email(), request.code());
    return buildResponse(payload);
  }

  private ResponseEntity<AuthResponse> buildResponse(AuthPayload payload) {
    HttpHeaders headers = new HttpHeaders();
    Duration refreshDuration = Duration.ofDays(properties.getRefreshTokenDays());
    refreshCookieService.addRefreshCookie(headers, payload.refreshToken(), refreshDuration);
    return ResponseEntity.ok().headers(headers).body(payload.response());
  }

  private String extractRefreshToken(HttpServletRequest request) {
    var cookie = WebUtils.getCookie(request, properties.getRefreshCookieName());
    if (cookie == null) {
      return null;
    }
    return cookie.getValue();
  }
}
