package com.lantanagroup.link.api.controller;

import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Bundle;
import org.hl7.fhir.r4.model.DocumentReference;

@Getter
@Setter
public class Report {
  String reportIdentifier;
  String measureIndentifier;
  String status;
  String docStatus;
  String author;
  String periodStartDate;
  String periodEndDate;
  String creationDate;

  public Report(Bundle.BundleEntryComponent bundle){
    DocumentReference docReference = (DocumentReference)bundle.getResource();
    if (!docReference.getAuthor().isEmpty()) {
      this.setAuthor(docReference.getAuthor().get(0).getReference());
    }
    this.setReportIdentifier(docReference.getMasterIdentifier().getValue());
    if (docReference.getStatus() != null) {
      this.setStatus(docReference.getStatus().getDisplay());
    }
    if (docReference.getDocStatus() != null) {
      this.setDocStatus(docReference.getDocStatus().getDisplay());
    }
    if (!docReference.getContent().isEmpty()) {
      this.setCreationDate((docReference.getContent().get(0)).getAttachment().getCreationElement().getValue().toString());
    }
    if (docReference.getContext() != null && docReference.getContext().getPeriod() != null && docReference.getContext().getPeriod().getEnd() != null) {
      this.setPeriodEndDate(docReference.getContext().getPeriod().getEnd().toString());
    }
    if (docReference.getContext() != null && docReference.getContext().getPeriod() != null && docReference.getContext().getPeriod().getStart() != null) {
      this.setPeriodStartDate(docReference.getContext().getPeriod().getStart().toString());
    }
    if (!docReference.getIdentifier().isEmpty()) {
      this.setMeasureIndentifier(docReference.getIdentifier().get(0).getSystem() + "|" + docReference.getIdentifier().get(0).getValue());
    }
  }
}
