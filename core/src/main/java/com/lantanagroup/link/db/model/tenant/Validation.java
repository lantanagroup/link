package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class Validation {
  private List<String> npmPackages = List.of("uscore.tgz", "qicore.tgz", "deqm.tgz");
}
