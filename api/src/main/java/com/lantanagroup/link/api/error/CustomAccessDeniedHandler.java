package com.lantanagroup.link.api.error;

import org.json.JSONObject;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAccessDeniedHandler implements AccessDeniedHandler {

  @Override
  public void handle(HttpServletRequest request, HttpServletResponse response, AccessDeniedException e) throws IOException, ServletException {
    response.setContentType("application/json;charset=UTF8");
    response.setStatus(401);
    response.getWriter().write(ErrorHelper.generateJSONErrorMessage("Lack valid credentials to perform request.", "401"));
  }
}
