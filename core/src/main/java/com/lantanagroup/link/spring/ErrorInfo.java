package com.lantanagroup.link.spring;

import org.springframework.http.HttpStatus;

import javax.servlet.ServletRequest;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

public class ErrorInfo {
  private static final String ATTRIBUTE_NAME = ErrorInfo.class.getName();
  private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSZ");
  private static final String MESSAGE_FORMAT = "An error occurred while processing your request; "
          + "for further assistance, please contact your system administrator with the following error ID: %s";

  public static ErrorInfo retrieve(ServletRequest request) {
    return (ErrorInfo)request.getAttribute(ATTRIBUTE_NAME);
  }

  private final Date timestamp = new Date();
  private final UUID errorId;
  private final HttpStatus status;
  private final String path;

  public ErrorInfo(HttpStatus status) {
    this.errorId = null;
    this.status = status;
    this.path = null;
  }

  public ErrorInfo(UUID errorId, HttpStatus status, String path) {
    this.errorId = errorId;
    this.status = status;
    this.path = path;
  }

  public void store(ServletRequest request) {
    request.setAttribute(ATTRIBUTE_NAME, this);
  }

  private String getMessage() {
    return status.is5xxServerError() && errorId != null
            ? String.format(MESSAGE_FORMAT, errorId)
            : status.getReasonPhrase();
  }

  public Map<String, Object> getProperties() {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("timestamp", TIMESTAMP_FORMAT.format(timestamp));
    properties.put("status", status.value());
    properties.put("error", status.getReasonPhrase());
    if (errorId != null) {
      properties.put("errorId", errorId);
    }
    properties.put("message", getMessage());
    if (path != null) {
      properties.put("path", path);
    }
    return properties;
  }
}
