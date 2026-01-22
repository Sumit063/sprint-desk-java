package com.sprintdesk.security;

import java.time.Duration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

@Component
public class RefreshCookieService {
  private final SecurityProperties properties;

  public RefreshCookieService(SecurityProperties properties) {
    this.properties = properties;
  }

  public String buildRefreshCookie(String value, Duration maxAge) {
    ResponseCookie.ResponseCookieBuilder builder =
        ResponseCookie.from(properties.getRefreshCookieName(), value)
            .httpOnly(true)
            .secure(properties.isRefreshCookieSecure())
            .sameSite(properties.getRefreshCookieSameSite())
            .path(properties.getRefreshCookiePath())
            .maxAge(maxAge);

    if (properties.getRefreshCookieDomain() != null
        && !properties.getRefreshCookieDomain().isBlank()) {
      builder.domain(properties.getRefreshCookieDomain());
    }

    return builder.build().toString();
  }

  public String clearCookie() {
    return buildRefreshCookie("", Duration.ZERO);
  }

  public void addRefreshCookie(HttpHeaders headers, String cookieValue, Duration maxAge) {
    headers.add(HttpHeaders.SET_COOKIE, buildRefreshCookie(cookieValue, maxAge));
  }

  public void clearCookie(HttpHeaders headers) {
    headers.add(HttpHeaders.SET_COOKIE, clearCookie());
  }
}