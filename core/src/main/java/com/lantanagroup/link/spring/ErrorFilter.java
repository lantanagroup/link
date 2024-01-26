package com.lantanagroup.link.spring;

import ca.uhn.fhir.rest.server.exceptions.BaseServerResponseException;
import com.auth0.jwk.JwkException;
import com.auth0.jwt.exceptions.JWTVerificationException;
import org.apache.http.client.HttpResponseException;
import org.springframework.core.Ordered;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.server.ResponseStatusException;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.UUID;

@Component
public class ErrorFilter extends OncePerRequestFilter implements Ordered {
  public static final String ERROR_ID = String.format("%s.error_id", ErrorFilter.class.getName());
  private static final String RESOLVED_MESSAGE = "Resolved unhandled %s to status code %d";
  private static final String UNRESOLVED_MESSAGE = "Caught unhandled %s and assigned error ID %s";

  @Override
  public int getOrder() {
    // Priority should be as high as possible, but lower than that of ErrorPageFilter
    // That way, we can catch exceptions before ErrorPageFilter swallows them
    // But we can still rely on ErrorPageFilter to forward requests to our custom error page
    return Ordered.HIGHEST_PRECEDENCE + 2;
  }

  @Override
  protected void doFilterInternal(
          @NonNull HttpServletRequest request,
          @NonNull HttpServletResponse response,
          @NonNull FilterChain chain)
          throws IOException {
    try {
      chain.doFilter(request, response);
    } catch (Exception e) {
      for (Throwable throwable = e; throwable != null; throwable = throwable.getCause()) {
        if (resolve(response, throwable)) {
          logger.debug(String.format(RESOLVED_MESSAGE, throwable.getClass().getSimpleName(), response.getStatus()));
          return;
        }
      }
      UUID errorId = UUID.randomUUID();
      logger.error(String.format(UNRESOLVED_MESSAGE, e.getClass().getSimpleName(), errorId), e);
      request.setAttribute(ERROR_ID, errorId);
      response.sendError(500);
    }
  }

  private boolean resolve(HttpServletResponse response, Throwable throwable) throws IOException {
    Integer statusCode = getStatusCode(throwable);
    if (statusCode != null) {
      response.sendError(statusCode, throwable.getMessage());
      return true;
    }
    if (throwable instanceof BaseServerResponseException) {
      BaseServerResponseException exception = (BaseServerResponseException) throwable;
      response.sendError(exception.getStatusCode(), exception.getMessage());
      return true;
    }
    if (throwable instanceof ResponseStatusException) {
      ResponseStatusException exception = (ResponseStatusException) throwable;
      response.sendError(exception.getStatus().value(), exception.getReason());
      return true;
    }
    if (throwable instanceof HttpResponseException) {
      HttpResponseException exception = (HttpResponseException) throwable;
      response.sendError(exception.getStatusCode(), exception.getReasonPhrase());
      return true;
    }
    return false;
  }

  private Integer getStatusCode(Throwable throwable) {
    if (throwable instanceof JwkException) {
      return 401;
    }
    if (throwable instanceof JWTVerificationException) {
      return 401;
    }
    return null;
  }
}
