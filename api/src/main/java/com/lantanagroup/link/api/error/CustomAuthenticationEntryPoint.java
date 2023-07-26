package com.lantanagroup.link.api.error;

import org.json.JSONObject;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;

public class CustomAuthenticationEntryPoint implements AuthenticationEntryPoint {

  //Unauthorized request handling with custom message
  @Override
  public void commence(HttpServletRequest request, HttpServletResponse response, AuthenticationException e) throws IOException, ServletException {
    response.setContentType("application/json;charset=UTF8");
    response.setStatus(403);
    response.getWriter().write(ErrorHelper.generateJSONErrorMessage("Unauthorized to perform request.", "403"));
  }
}
