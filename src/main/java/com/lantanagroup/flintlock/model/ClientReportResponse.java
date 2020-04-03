package com.lantanagroup.flintlock.model;

import org.hl7.fhir.r4.model.Bundle;

import java.util.List;

public class ClientReportResponse {
  private String bundle;
  private List<SimplePosition> positions;

  public List<SimplePosition> getPositions() {
    return positions;
  }

  public void setPositions(List<SimplePosition> positions) {
    this.positions = positions;
  }

  public String getBundle() {
    return bundle;
  }

  public void setBundle(String bundle) {
    this.bundle = bundle;
  }
}
