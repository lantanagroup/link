package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;
import org.springframework.validation.annotation.Validated;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Getter
@Setter
@Validated
public class ApiMeasurePackage {

  @NotBlank
  private String id;

  @NotNull
  @Size(min = 1)
  private String[] bundleIds;

}
