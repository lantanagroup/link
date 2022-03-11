package com.lantanagroup.link.config.consumer;

import lombok.Getter;
import lombok.Setter;


@Getter
@Setter
public class Permission {
  String resourceType;
  Role[] roles;
}
