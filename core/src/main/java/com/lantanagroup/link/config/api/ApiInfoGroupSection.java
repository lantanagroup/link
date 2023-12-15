package com.lantanagroup.link.config.api;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
public class ApiInfoGroupSection {
  private String name;
  private List<ApiInfoGroupSectionLink> links = new ArrayList<>();
}
