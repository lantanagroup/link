package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class Validation {
  private List<String> npmPackages = new ArrayList<>(List.of("uscore.tgz", "qicore.tgz", "deqm.tgz"));
}
