package com.lantanagroup.link.config;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Permission {
  String resourceType;
  Role[] roles;
}
