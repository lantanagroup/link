package com.lantanagroup.link.auth;

public interface ITokenValidator {
  boolean verifyToken(String authHeader, String algorithm, String issuer, String jwksUrl, String validationEndpoint);
}
