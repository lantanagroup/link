package com.lantanagroup.link.model;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@JsonInclude(JsonInclude.Include.NON_NULL)
@NoArgsConstructor
public class ApiVersionModel {
  private String build = "dev";
  private String version;
  private String commit;

  @JsonProperty("cqf-version")
  private String cqfVersion;

  public ApiVersionModel(String cqfVersion) {
    this.cqfVersion = cqfVersion;
  }
}
