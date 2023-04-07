package com.lantanagroup.link.db.model.tenant;

import com.google.common.base.Strings;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.StringType;

@Getter
@Setter
public class Address {
  private String addressLine;
  private String city;
  private String state;
  private String postalCode;
  private String country;

  public org.hl7.fhir.r4.model.Address getFHIRAddress() {
    org.hl7.fhir.r4.model.Address ret = new org.hl7.fhir.r4.model.Address();

    if (!Strings.isNullOrEmpty(this.getAddressLine())) {
      ret.getLine().add(new StringType(this.getAddressLine()));
    }

    if (!Strings.isNullOrEmpty(this.getCity())) {
      ret.setCity(this.getCity());
    }

    if (!Strings.isNullOrEmpty(this.getState())) {
      ret.setState(this.getState());
    }

    if (!Strings.isNullOrEmpty(this.getPostalCode())) {
      ret.setPostalCode(this.getPostalCode());
    }

    if (!Strings.isNullOrEmpty(this.getCountry())) {
      ret.setCountry(this.getCountry());
    }

    return ret;
  }
}
