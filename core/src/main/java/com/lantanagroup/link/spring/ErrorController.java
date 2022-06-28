package com.lantanagroup.link.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {
  @Value("${server.error.path:/error}")
  private String errorPath;

  @Override
  public String getErrorPath() {
    return errorPath;
  }

  @RequestMapping("${server.error.path:/error}")
  public Map<String, Object> handleError(HttpServletRequest request, HttpServletResponse response) {
    ErrorInfo errorInfo = ErrorInfo.retrieve(request);
    if (errorInfo == null) {
      errorInfo = new ErrorInfo(HttpStatus.valueOf(response.getStatus()));
    }
    return errorInfo.getProperties();
  }
}
