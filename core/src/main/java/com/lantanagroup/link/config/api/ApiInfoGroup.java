package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ApiInfoGroup {
  private String name;
  private List<ApiInfoGroupSection> sections = new ArrayList<>();
}
