package com.lantanagroup.link.spring;

import com.auth0.jwt.exceptions.JWTVerificationException;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class ErrorFilter extends OncePerRequestFilter {
  private final Map<Class<? extends Exception>, HttpStatus> statusesByExceptionType;

  public ErrorFilter() {
    statusesByExceptionType = new LinkedHashMap<>();
    statusesByExceptionType.put(JWTVerificationException.class, HttpStatus.UNAUTHORIZED);
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
      UUID errorId = UUID.randomUUID();
      logger.error(String.format("Unhandled exception: %s", errorId), e);
      HttpStatus status = getStatus(e);
      ErrorInfo errorInfo = new ErrorInfo(errorId, status, request.getServletPath());
      errorInfo.store(request);
      response.sendError(status.value(), status.getReasonPhrase());
    }
  }

  private HttpStatus getStatus(Exception exception) {
    for (Throwable throwable = exception; throwable != null; throwable = throwable.getCause()) {
      for (Map.Entry<Class<? extends Exception>, HttpStatus> statusByExceptionType
              : statusesByExceptionType.entrySet()) {
        if (statusByExceptionType.getKey().isInstance(throwable)) {
          return statusByExceptionType.getValue();
        }
      }
    }
    return HttpStatus.INTERNAL_SERVER_ERROR;
  }
}
