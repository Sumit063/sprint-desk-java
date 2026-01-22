package com.sprintdesk.service;

import com.sprintdesk.security.SecurityProperties;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class GoogleAuthService {
  /**
   * Validates Google ID tokens using the tokeninfo endpoint.
   */
  private final SecurityProperties securityProperties;
  private final RestTemplate restTemplate = new RestTemplate();

  public GoogleAuthService(SecurityProperties securityProperties) {
    this.securityProperties = securityProperties;
  }

  public GoogleProfile verify(String idToken) {
    if (!securityProperties.isGoogleEnabled()
        || securityProperties.getGoogleClientId() == null
        || securityProperties.getGoogleClientId().isBlank()) {
      throw new ResponseStatusException(HttpStatus.NOT_IMPLEMENTED, "Google auth not configured");
    }
    if (idToken == null || idToken.isBlank()) {
      throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Missing credential");
    }

    GoogleTokenInfo info;
    try {
      info =
          restTemplate.getForObject(
              "https://oauth2.googleapis.com/tokeninfo?id_token={token}",
              GoogleTokenInfo.class,
              idToken);
    } catch (RestClientException ex) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential");
    }

    if (info == null
        || info.email() == null
        || info.aud() == null
        || !securityProperties.getGoogleClientId().equals(info.aud())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid Google credential");
    }

    if (info.emailVerified() != null && !"true".equalsIgnoreCase(info.emailVerified())) {
      throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Email not verified");
    }

    return new GoogleProfile(info.email(), info.name(), info.picture());
  }

  public record GoogleProfile(String email, String name, String avatarUrl) {}

  private record GoogleTokenInfo(
      String aud,
      String email,
      String emailVerified,
      String name,
      String picture) {}
}
