package com.lantanagroup.link.model;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

import java.util.Date;

@Getter
@Setter
public class Report {
  String id;
  String measureIdentifier;
  String status;
  String docStatus;
  String author;
  Date periodStartDate;
  Date periodEndDate;
  Date creationDate;

  public Report(Bundle.BundleEntryComponent bundle){
    DocumentReference docReference = (DocumentReference)bundle.getResource();
    if (!docReference.getAuthor().isEmpty()) {
      this.setAuthor(docReference.getAuthor().get(0).getReference());
    }
    this.setId(docReference.getMasterIdentifier().getValue());
    if (docReference.getStatus() != null) {
      this.setStatus(docReference.getStatus().toCode());
    }
    if (docReference.getDocStatus() != null) {
      this.setDocStatus(docReference.getDocStatus().toCode());
    }
    if (!docReference.getContent().isEmpty()) {
      this.setCreationDate((docReference.getContent().get(0)).getAttachment().getCreationElement().getValue());
    }
    if (docReference.getContext() != null && docReference.getContext().getPeriod() != null && docReference.getContext().getPeriod().getEnd() != null) {
      this.setPeriodEndDate(docReference.getContext().getPeriod().getEnd());
    }
    if (docReference.getContext() != null && docReference.getContext().getPeriod() != null && docReference.getContext().getPeriod().getStart() != null) {
      this.setPeriodStartDate(docReference.getContext().getPeriod().getStart());
    }
    if (!docReference.getIdentifier().isEmpty()) {
      this.setMeasureIdentifier(docReference.getIdentifier().get(0).getSystem() + "|" + docReference.getIdentifier().get(0).getValue());
    }
  }
}
