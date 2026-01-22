package com.sprintdesk.security;

import java.util.UUID;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

public final class SecurityUtils {
  private SecurityUtils() {}

  public static UUID getCurrentUserId() {
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || !authentication.isAuthenticated()) {
      return null;
    }
    Object principal = authentication.getPrincipal();
    if (principal instanceof UserPrincipal userPrincipal) {
      return userPrincipal.getUserId();
    }
    if (principal instanceof String value) {
      try {
        return UUID.fromString(value);
      } catch (IllegalArgumentException ex) {
        return null;
      }
    }
    return null;
  }
}