package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Practitioner;

@Getter
@Setter
public class UserModel {
  String id;
  String name;

  public UserModel(IBaseResource resource){
    Practitioner practitioner = (Practitioner)resource;
    String  id = practitioner.getId();
    String idValue = id.substring(id.indexOf("Practitioner"), id.indexOf("_history")-1);
    if(!practitioner.getId().isEmpty()){
      this.setId(idValue);
      this.setName(practitioner.getName().get(0).getGiven().get(0).getValue().replaceAll("\"","") + " " +  practitioner.getName().get(0).getFamily().replaceAll("\"", "") );
    }
  }
}
