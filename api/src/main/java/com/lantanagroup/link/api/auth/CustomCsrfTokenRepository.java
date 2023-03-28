package com.lantanagroup.link.api.auth;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.DefaultCsrfToken;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.util.WebUtils;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.UUID;

@Component
public class CustomCsrfTokenRepository implements CsrfTokenRepository {
  static final String DEFAULT_CSRF_COOKIE_NAME = "XSRF-TOKEN";
  static final String DEFAULT_CSRF_PARAMETER_NAME = "_csrf";
  static final String DEFAULT_CSRF_HEADER_NAME = "X-XSRF-TOKEN";

  private String parameterName = DEFAULT_CSRF_PARAMETER_NAME;
  private String headerName = DEFAULT_CSRF_HEADER_NAME;
  private String cookieName = DEFAULT_CSRF_COOKIE_NAME;
  private boolean cookieHttpOnly = true;
  private String cookiePath;
  private Boolean secure;

  public CustomCsrfTokenRepository() {
  }

  @Override
  public CsrfToken generateToken(HttpServletRequest httpServletRequest) {
    return new DefaultCsrfToken(this.headerName, this.parameterName, createNewToken());
  }

  private String createNewToken() {
    return UUID.randomUUID().toString();
  }

  @Override
  public void saveToken(CsrfToken token, HttpServletRequest request, HttpServletResponse response) {

    String tokenValue = (token != null) ? token.getToken() : "";
    ResponseCookie.ResponseCookieBuilder cookieB = ResponseCookie.from(this.cookieName, tokenValue)
            .maxAge(3600) // one hour
            .httpOnly(false)
            .path(StringUtils.hasLength(this.cookiePath) ? this.cookiePath : this.getRequestContext(request));

    //if not running on localhost, then add secure and sameSite attributes
    if(!request.getRequestURL().toString().contains("localhost")) {
      cookieB.secure(true).sameSite("None");
    }

    ResponseCookie cookie = cookieB.build();
    response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());

  }

  @Override
  public CsrfToken loadToken(HttpServletRequest httpServletRequest) {
    Cookie cookie = WebUtils.getCookie(httpServletRequest, this.cookieName);
    if (cookie == null) {
      return null;
    }
    String token = cookie.getValue();
    if (!StringUtils.hasLength(token)) {
      return null;
    }
    return new DefaultCsrfToken(this.headerName, this.parameterName, token);
  }

  public void setCookieHttpOnly(boolean cookieHttpOnly) {
    this.cookieHttpOnly = cookieHttpOnly;
  }

  public void setSecure(Boolean secure) {
    this.secure = secure;
  }

  private String getRequestContext(HttpServletRequest request) {
    String contextPath = request.getContextPath();
    return (contextPath.length() > 0) ? contextPath : "/";
  }

}
