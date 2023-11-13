package com.lantanagroup.link.db.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;

import java.util.UUID;

@Getter
@Setter
public class DataTrace {
  private static final String userDataKey = DataTrace.class.getName() + ".id";

  private UUID id;
  private UUID queryId;
  private String patientId;
  private String resourceType;
  private String resourceId;
  private String originalResource;

  public static void setId(IBaseResource resource, UUID id) {
    resource.setUserData(userDataKey, id);
  }

  public static UUID getId(IBaseResource resource) {
    Object id = resource.getUserData(userDataKey);
    return id instanceof UUID ? (UUID) id : null;
  }
}
