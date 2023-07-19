package com.lantanagroup.link.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiInfoModel {
  private String build = "dev";
  private String version;
  private String commit;

  @JsonProperty("cqf-version")
  private String cqfVersion;
}
