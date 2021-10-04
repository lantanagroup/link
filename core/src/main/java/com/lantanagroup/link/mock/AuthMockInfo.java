package com.lantanagroup.link.mock;

import com.lantanagroup.link.auth.LinkCredentials;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.core.Authentication;

@Getter @Setter
public class AuthMockInfo {
  private Authentication authentication;
  private LinkCredentials user;
}
