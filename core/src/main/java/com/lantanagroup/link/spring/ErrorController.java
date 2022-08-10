package com.lantanagroup.link.spring;

import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.RequestDispatcher;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping({"${server.error.path:${error.path:/error}}"})
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
  private static final SimpleDateFormat TIMESTAMP_FORMAT = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
  private static final String MESSAGE_WITHOUT_ID = "An error occurred while processing your request; "
          + "for further assistance, please contact your system administrator";
  private static final String MESSAGE_WITH_ID = MESSAGE_WITHOUT_ID + " with the following error ID: %s";

  @Value("${error.path:/error}")
  private String errorPath = "/error";

  @Override
  public String getErrorPath() {
    return errorPath;
  }

  @RequestMapping
  public Map<String, Object> handle(HttpServletRequest request, HttpServletResponse response) {
    Map<String, Object> properties = new LinkedHashMap<>();
    properties.put("timestamp", TIMESTAMP_FORMAT.format(new Date()));
    properties.put("status", response.getStatus());
    String message = getMessage(request, response);
    if (StringUtils.isNotEmpty(message)) {
      properties.put("error", message);
    }
    String path = (String) request.getAttribute(RequestDispatcher.ERROR_REQUEST_URI);
    if (StringUtils.isNotEmpty(path)) {
      properties.put("path", path);
    }
    return properties;
  }

  private String getMessage(HttpServletRequest request, HttpServletResponse response) {
    if (response.getStatus() == HttpStatus.INTERNAL_SERVER_ERROR.value()) {
      UUID errorId = (UUID) request.getAttribute(ErrorFilter.ERROR_ID);
      return errorId != null ? String.format(MESSAGE_WITH_ID, errorId) : MESSAGE_WITHOUT_ID;
    }
    String message = (String) request.getAttribute(RequestDispatcher.ERROR_MESSAGE);
    if (StringUtils.isNotEmpty(message)) {
      return message;
    }
    HttpStatus status = HttpStatus.resolve(response.getStatus());
    if (status != null) {
      return status.getReasonPhrase();
    }
    return null;
  }
}
