package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Validated
public class ApiMeasurePackage {

  @NotBlank
  private String id;

  private String[] bundleIds;

}
