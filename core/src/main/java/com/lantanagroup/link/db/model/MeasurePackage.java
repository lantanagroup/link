package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter
@Setter
public class MeasurePackage {
  private String id;
  private List<String> measureIds;
}
