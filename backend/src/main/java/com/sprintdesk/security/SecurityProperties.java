package com.sprintdesk.security;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "app.security")
public class SecurityProperties {
  private String jwtSecret;
  private long accessTokenMinutes = 15;
  private long refreshTokenDays = 7;
  private String refreshCookieName = "sprintdesk_refresh";
  private boolean refreshCookieSecure = false;
  private String refreshCookieSameSite = "Lax";
  private String refreshCookiePath = "/";
  private String refreshCookieDomain;
  private boolean googleEnabled = false;
  private String googleClientId;
  private int otpMinutes = 10;
  private int otpCodeLength = 6;
  private boolean otpReturnCode = false;

  public String getJwtSecret() {
    return jwtSecret;
  }

  public void setJwtSecret(String jwtSecret) {
    this.jwtSecret = jwtSecret;
  }

  public long getAccessTokenMinutes() {
    return accessTokenMinutes;
  }

  public void setAccessTokenMinutes(long accessTokenMinutes) {
    this.accessTokenMinutes = accessTokenMinutes;
  }

  public long getRefreshTokenDays() {
    return refreshTokenDays;
  }

  public void setRefreshTokenDays(long refreshTokenDays) {
    this.refreshTokenDays = refreshTokenDays;
  }

  public String getRefreshCookieName() {
    return refreshCookieName;
  }

  public void setRefreshCookieName(String refreshCookieName) {
    this.refreshCookieName = refreshCookieName;
  }

  public boolean isRefreshCookieSecure() {
    return refreshCookieSecure;
  }

  public void setRefreshCookieSecure(boolean refreshCookieSecure) {
    this.refreshCookieSecure = refreshCookieSecure;
  }

  public String getRefreshCookieSameSite() {
    return refreshCookieSameSite;
  }

  public void setRefreshCookieSameSite(String refreshCookieSameSite) {
    this.refreshCookieSameSite = refreshCookieSameSite;
  }

  public String getRefreshCookiePath() {
    return refreshCookiePath;
  }

  public void setRefreshCookiePath(String refreshCookiePath) {
    this.refreshCookiePath = refreshCookiePath;
  }

  public String getRefreshCookieDomain() {
    return refreshCookieDomain;
  }

  public void setRefreshCookieDomain(String refreshCookieDomain) {
    this.refreshCookieDomain = refreshCookieDomain;
  }

  public boolean isGoogleEnabled() {
    return googleEnabled;
  }

  public void setGoogleEnabled(boolean googleEnabled) {
    this.googleEnabled = googleEnabled;
  }

  public String getGoogleClientId() {
    return googleClientId;
  }

  public void setGoogleClientId(String googleClientId) {
    this.googleClientId = googleClientId;
  }

  public int getOtpMinutes() {
    return otpMinutes;
  }

  public void setOtpMinutes(int otpMinutes) {
    this.otpMinutes = otpMinutes;
  }

  public int getOtpCodeLength() {
    return otpCodeLength;
  }

  public void setOtpCodeLength(int otpCodeLength) {
    this.otpCodeLength = otpCodeLength;
  }

  public boolean isOtpReturnCode() {
    return otpReturnCode;
  }

  public void setOtpReturnCode(boolean otpReturnCode) {
    this.otpReturnCode = otpReturnCode;
  }
}
