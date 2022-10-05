package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.hl7.fhir.r4.model.HumanName;
import org.hl7.fhir.r4.model.Practitioner;

@Getter
@Setter
public class UserModel {
  String id;
  String name;

  public UserModel(Practitioner practitioner) {
    if (practitioner != null) {
      if (practitioner.getIdElement() != null && StringUtils.isNotEmpty(practitioner.getIdElement().getIdPart())) {
        this.setId(practitioner.getIdElement().getIdPart());
      }

      if (practitioner.getName() != null && !practitioner.getName().isEmpty()) {
        HumanName name = practitioner.getNameFirstRep();
        String given = name.getGiven() != null && !name.getGiven().isEmpty() ? name.getGiven().get(0).getValue() : null;

        if (StringUtils.isNotEmpty(given) && StringUtils.isNotEmpty(name.getFamily())) {
          this.setName(String.format("%s %s", given.replaceAll("\"", ""), name.getFamily().replaceAll("\"", "")));
        } else if (StringUtils.isNotEmpty(given)) {
          this.setName(given.replaceAll("\"", ""));
        } else if (StringUtils.isNotEmpty(name.getFamily())) {
          this.setName(name.getFamily().replaceAll("\"", ""));
        }
      }
    }
  }
}
