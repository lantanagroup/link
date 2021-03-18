package com.lantanagroup.nandina.api.model;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class SimplePosition {
  private double latitude;
  private double longitude;
  private int strength = 1;
}
