package com.lantanagroup.link.db.model.tenant;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Address {
  private String addressLine;
  private String city;
  private String state;
  private String postalCode;
  private String country;
}
