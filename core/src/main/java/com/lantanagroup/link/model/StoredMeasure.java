package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a
 */
@Getter
@Setter
public class StoredMeasure {
  private String id;
  private String name;
  private List<String> bundleIds = new ArrayList<>();
}
