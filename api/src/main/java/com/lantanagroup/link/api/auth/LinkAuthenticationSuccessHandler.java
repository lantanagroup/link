package com.lantanagroup.link.api.auth;

import com.lantanagroup.link.auth.LinkCredentials;
import com.lantanagroup.link.db.MongoService;
import com.lantanagroup.link.db.model.User;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

public class LinkAuthenticationSuccessHandler implements AuthenticationSuccessHandler {
  private static final Logger logger = LoggerFactory.getLogger(LinkAuthenticationSuccessHandler.class);
  private MongoService mongoService;

  public LinkAuthenticationSuccessHandler(MongoService mongoService) {
    this.mongoService = mongoService;
  }

  @Override
  public void onAuthenticationSuccess(HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse, Authentication authentication) {
    LinkCredentials credentials = (LinkCredentials) authentication.getPrincipal();
    User found = this.mongoService.getUser(credentials.getJwt().getSubject());

    if (found == null) {
      logger.info("User in JWT not found, creating user id {}", credentials.getJwt().getSubject());

      User user = new User();
      user.setId(credentials.getJwt().getSubject());

      if (credentials.getJwt().getClaim("name") != null) {
        user.setName(credentials.getJwt().getClaim("name").asString());
      }

      if (credentials.getJwt().getClaim("email") != null) {
        user.setEmail(credentials.getJwt().getClaim("email").asString());
      }

      this.mongoService.saveUser(user);
      credentials.setUser(user);
    } else {
      credentials.setUser(found);
    }
  }
}
