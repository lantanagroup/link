package com.lantanagroup.link.spring;

import com.fasterxml.jackson.annotation.JsonInclude;
import org.apache.commons.lang3.StringUtils;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.servlet.DispatcherServlet;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@JsonInclude(JsonInclude.Include.NON_NULL)
public class ErrorInfo {
  private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private static final String MESSAGE_WITHOUT_ID = "An error occurred while processing your request; "
          + "for further assistance, please contact your system administrator";
  private static final String MESSAGE_WITH_ID = MESSAGE_WITHOUT_ID + " with the following error ID: %s";

  private final Date timestamp = new Date();
  private final int statusCode;
  private final UUID errorId;
  private final String message;
  private final String path;

  public ErrorInfo(HttpServletRequest request, HttpServletResponse response) {
    statusCode = response.getStatus();
    errorId = (UUID) request.getAttribute(ErrorFilter.ERROR_ID);
    message = this.getMessage(request);
    path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
  }

  private String getMessage(HttpServletRequest request) {
    String msg = null;

    if (request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE) != null) {
      Exception ex = (Exception) request.getAttribute(DispatcherServlet.EXCEPTION_ATTRIBUTE);

      // Only for certain exceptions should we return the details of the issue
      if (ex instanceof HttpMessageNotReadableException) {
        HttpMessageNotReadableException nrex = (HttpMessageNotReadableException) ex;
        msg = nrex.getMessage();
      }
    }

    // Fall back to the error message in the request dispatcher, if any
    if (StringUtils.isEmpty(msg)) {
      msg = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    }

    return msg;
  }

  public Map<String, Object> getProperties() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("timestamp", getTimestamp());
    properties.put("status", statusCode);
    String error = getError();
    if (error != null) {
      properties.put("error", error);
    }
    if (errorId != null) {
      properties.put("errorId", errorId);
    }
    String message = getMessage();
    if (message != null) {
      properties.put("message", message);
    }
    if (path != null) {
      properties.put("path", path);
    }
    return properties;
  }

  private String getTimestamp() {
    return TIMESTAMP_FORMAT.format(timestamp);
  }

  private String getError() {
    HttpStatus status = HttpStatus.resolve(statusCode);
    return status != null ? status.getReasonPhrase() : null;
  }

  private String getMessage() {
    if (statusCode == 500) {
      return errorId != null ? String.format(MESSAGE_WITH_ID, errorId) : MESSAGE_WITHOUT_ID;
    }
    return message;
  }
}
