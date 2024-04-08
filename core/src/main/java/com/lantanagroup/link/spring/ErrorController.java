package com.lantanagroup.link.spring;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Map;

@RestController
@RequestMapping({"${server.error.path:${error.path:/error}}"})
public class ErrorController implements org.springframework.boot.web.servlet.error.ErrorController {

  @RequestMapping
  public Map<String, Object> handle(HttpServletRequest request, HttpServletResponse response) {
    return new ErrorInfo(request, response).getProperties();
  }
}
