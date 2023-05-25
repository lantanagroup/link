package com.lantanagroup.link.auth;

import com.auth0.jwt.interfaces.DecodedJWT;
import com.lantanagroup.link.db.model.User;
import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class LinkCredentials {
  DecodedJWT jwt;
  User user;
}
