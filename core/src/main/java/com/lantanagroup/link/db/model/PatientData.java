package com.lantanagroup.link.db.model;

import com.lantanagroup.link.Constants;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseMetaType;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;

import java.util.Date;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Getter
@Setter
public class PatientData {
  private UUID id;
  private UUID dataTraceId;
  private String patientId;
  private String resourceType;
  private String resourceId;
  private IBaseResource resource;
  private Date retrieved;

  public static Bundle asBundle(List<PatientData> patientData) {
    Bundle bundle = new Bundle();
    bundle.setEntry(patientData.stream().map(pd -> {
      Bundle.BundleEntryComponent newEntry = new Bundle.BundleEntryComponent();
      newEntry.setResource((Resource) pd.getResource());
      return newEntry;
    }).collect(Collectors.toList()));
    return bundle;
  }

  public void setResource(IBaseResource resource) {
    this.resource = resource;
    if (retrieved == null) {
      retrieved = getRetrievedFromResource();
    }
  }

  private Date getRetrievedFromResource() {
    if (resource == null) {
      return null;
    }
    IBaseMetaType meta = resource.getMeta();
    if (!(meta instanceof Meta)) {
      return null;
    }
    Extension extension = ((Meta) meta).getExtensionByUrl(Constants.ReceivedDateExtensionUrl);
    if (extension == null) {
      return null;
    }
    Type value = extension.getValue();
    if (!(value instanceof DateTimeType)) {
      return null;
    }
    return ((DateTimeType) value).getValue();
  }
}
