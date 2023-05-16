package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ApiInfoModel {
  private String build;
  private String version;

  public ApiInfoModel() {

  }

  public ApiInfoModel(String build, String version) {
    this.setBuild(build);
    this.setVersion(version);
  }
}
